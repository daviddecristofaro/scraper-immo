package it.ibee.scraperimmo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import it.ibee.scraperimmo.model.ComuneImmo;
import it.ibee.scraperimmo.model.ProvinciaImmo;
import it.ibee.scraperimmo.model.ReAds;
import it.ibee.scraperimmo.model.SingleAd;
import it.ibee.scraperimmo.model.repository.ProvinciaImmoRepository;
import it.ibee.scraperimmo.model.repository.ReAdsRepository;
import it.ibee.scraperimmo.service.ConnectionService;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class Scheduler {

    private static final String IMMO_IT = "https://www.immobiliare.it/";
    private static final String ND_LISTMETA_LINK = "nd-listMeta__link";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String PAG_SUFFIX = "?pag=";
    private static final String IN_CARD_TITLE = "in-card__title";
    private static final String IM_TITLE_BLOCK_TITLE = "im-titleBlock__title";
    private static final String IM_MAINFEATURES_TITLE = "im-mainFeatures__title";
    private static final String IM_LOCATION = "im-location";
    private static final String IM_FEATURES_VALUE = "im-features__value";
    private static final String IM_STRUCTURE_MAINCONTENT = "im-structure__mainContent";

    private static final String GEOCODE_LINK = "https://geocode.xyz/{request}?&auth=239970491373371841244x13603&geojson=1";

    private static final String[] TACTICAL_CITIES = {"milano", "roma", "bari", "bologna"};
    @Autowired
    ProvinciaImmoRepository provinciaImmoRepository;
    @Autowired
    ReAdsRepository reAdsRepository;
    @Autowired
    ConnectionService connectionService;
    private Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    @Scheduled(fixedRate = 60 * 60 * 72000) //72 hours
    private void importProvince() {
        try {
            Document doc = Jsoup.connect(IMMO_IT).get();
            Elements listMeta__link = doc.getElementsByClass(ND_LISTMETA_LINK);

            listMeta__link.stream().parallel().forEach(element -> {
                String link = element.attr(HREF_ATTRIBUTE);
                if (link.indexOf("comuni") != -1)
                    if (provinciaImmoRepository.findFirstByLink(link) != null) {
                        LOGGER.info("Already exist provinciaImmo with link {}", link);
                    } else {
                        ProvinciaImmo pi = new ProvinciaImmo();
                        pi.setLink(link);
                        provinciaImmoRepository.save(pi);
                        LOGGER.info("Saved provinciaImmo {}", pi.getLink());
                    }
            });


        } catch (IOException e) {
            LOGGER.error("Error during scheduler", e);
        }

        this.importComuni();
    }

    private void importComuni() {
        //nd-listMeta__link
        List<ProvinciaImmo> pis = provinciaImmoRepository.findAllByProcessed(false);

        pis.stream().parallel().forEach(pi -> {
            try {
                Document doc = Jsoup.connect(pi.getLink()).get();
                Elements listMeta__link = doc.getElementsByClass(ND_LISTMETA_LINK);

                listMeta__link.stream().forEach(element -> {
                    String link = element.attr(HREF_ATTRIBUTE);
                    if (!pi.getComuneImmos().stream().anyMatch(ci -> ci.getLink().equals(link))) {
                        ComuneImmo ci = new ComuneImmo();
                        ci.setLink(link);
                        pi.getComuneImmos().add(ci);
                        LOGGER.info("Saved comuneImmo {}", ci.getLink());
                    } else
                        LOGGER.info("Already exist comuneImmo with link {}", link);
                });
                LOGGER.info("Updated provinciaImmo {}", pi.getLink());
            } catch (IOException e) {
                LOGGER.error("Error during get page {}", pi.getLink(), e);
            } catch (Exception e) {
                LOGGER.error("Generic error during importComuni", e);
            }
        });
        provinciaImmoRepository.saveAll(pis);
        this.importSingleAds();
    }

    private void importSingleAds() {
        List<ProvinciaImmo> pis = provinciaImmoRepository.findAllByProcessed(false);

        pis.forEach(pi -> {

            pi.getComuneImmos().stream().filter(ci -> !ci.getProcessed()).parallel().forEach(ci -> {
                boolean endOfPages = false;
                int page = 1;
                AtomicInteger saCount = new AtomicInteger();
                try {
                    do {
                        try { // !!! Perdoname madre por mi coding loca !!!
                            Document doc = Jsoup.connect(ci.getLink().concat(PAG_SUFFIX).concat(String.valueOf(page))).get();
                            LOGGER.info("Parsing page {} in comuneImmo {}", page, ci.getLink());
                            //ci.setSingleAds(new ArrayList<>());
                            Elements listMeta__link = doc.getElementsByClass(IN_CARD_TITLE);
                            listMeta__link.stream().forEach(element -> {
                                String link = element.attr(HREF_ATTRIBUTE);
                                if (link.contains("annunci")) {
                                    SingleAd sa = new SingleAd();
                                    sa.setLink(link);
                                    ci.getSingleAds().add(sa);
                                    saCount.getAndIncrement();
                                }
                            });

                        } catch (HttpStatusException e) {
                            endOfPages = true;
                            LOGGER.info("End Parsing at page {} for comuneImmo {}", page, ci.getLink());
                        }
                        page++;
                    } while (!endOfPages);
                    LOGGER.info("Inserted {} in comuneImmo {}", saCount, ci.getLink());
                } catch (IOException e) {
                    LOGGER.error("Error during get page {}", ci.getLink(), e);
                } catch (Exception e) {
                    LOGGER.error("Generic error during importSingleAds", e);
                }
            });
            LOGGER.info("<----- Updated provinciaImmo {} with ads ----->", pi.getLink());
        });

        provinciaImmoRepository.saveAll(pis);

        this.scrapeAd();
    }

    //@Scheduled(fixedRate = 60 * 60 * 72000) //72 hours
    private void scrapeAd() {
        //this.resetProcessing();
        List<ProvinciaImmo> pis = provinciaImmoRepository.findAllByProcessed(false);
        pis.stream().filter(pi -> Arrays.stream(TACTICAL_CITIES).anyMatch(pi.getLink()::contains)).forEach(pi -> {
            pi.setStartProcessing(new Date());
            pi.getComuneImmos().stream().filter(ci -> !ci.getProcessed()).forEach(ci -> {
                ci.setStartProcessing(new Date());

                int l = ci.getSingleAds().size();
                if (l >= 10) {
                    ExecutorService executorService = Executors.newFixedThreadPool(5);
                    List<List<SingleAd>> chunks = Lists.partition(ci.getSingleAds().stream().filter(sa -> !sa.getProcessed()).collect(Collectors.toList()),
                            Integer.valueOf(l / 5));

                    ListeningExecutorService listeningExecutorService =
                            MoreExecutors.listeningDecorator(executorService);
                    CountDownLatch latch = new CountDownLatch(5);

                    if (chunks.size() > 0) {
                        /*
                        THREAD 0
                         */
                        if (chunks.get(0) != null)
                            listeningExecutorService.execute(() -> {
                                this.scrapeSingleAd(chunks.get(0), pi.getLink(), ci.getLink());//chunks.set(0, );
                                latch.countDown();
                            });
                        /*
                        THREAD 1
                         */
                        if (chunks.get(1) != null)
                            listeningExecutorService.execute(() -> {
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    LOGGER.error("Error in thread 1 sleep", e);
                                }
                                this.scrapeSingleAd(chunks.get(1), pi.getLink(), ci.getLink()); //chunks.set(1, );
                                latch.countDown();
                            });
                        /*
                        THREAD 2
                         */
                        if (chunks.get(2) != null)
                            listeningExecutorService.execute(() -> {
                                try {
                                    Thread.sleep(1500);
                                } catch (InterruptedException e) {
                                    LOGGER.error("Error in thread 2 sleep", e);
                                }
                                this.scrapeSingleAd(chunks.get(2), pi.getLink(), ci.getLink());//chunks.set(2, );
                                latch.countDown();
                            });
                        /*
                        THREAD 3
                         */
                        if (chunks.get(3) != null)
                            listeningExecutorService.execute(() -> {
                                try {
                                    Thread.sleep(2500);
                                } catch (InterruptedException e) {
                                    LOGGER.error("Error in thread 3 sleep", e);
                                }
                                this.scrapeSingleAd(chunks.get(3), pi.getLink(), ci.getLink());//chunks.set(3, );
                                latch.countDown();
                            });
                        /*
                        THREAD 4
                         */
                        if (chunks.get(4) != null)
                            listeningExecutorService.execute(() -> {
                                try {
                                    Thread.sleep(3500);
                                } catch (InterruptedException e) {
                                    LOGGER.error("Error in thread 4 sleep", e);
                                }
                                try {
                                    this.scrapeSingleAd(chunks.get(4), pi.getLink(), ci.getLink());//chunks.set(4, );
                                } catch (Exception e) {
                                    LOGGER.error("ERROR", e);
                                }
                                latch.countDown();
                            });

                        try {
                            while (!latch.await(1000, TimeUnit.MILLISECONDS)) {
                                Thread.sleep(500);
                            }
                            List<SingleAd> merged = new ArrayList<>();
                            for (List<SingleAd> sas : chunks) {
                                merged.addAll(sas.stream().filter(sa -> !sa.getExpired()).collect(Collectors.toList()));
                            }
                            ci.setSingleAds(merged);
                            this.updateProvinciaComune(pi, ci);
                        } catch (InterruptedException e) {
                            LOGGER.error("Error in latch thread sleep", e);
                        }
                    }
                } else {
                    this.scrapeSingleAd(ci.getSingleAds(), pi.getLink(), ci.getLink());
                    /*ci.setSingleAds(this.scrapeSingleAd(ci.getSingleAds(), pi.getLink(), ci.getLink())
                            .stream().filter(sa -> !sa.getExpired()).collect(Collectors.toList()));*/
                    this.updateProvinciaComune(pi, ci);
                }
            });
        });
    }

    private void scrapeSingleAd(List<SingleAd> sas, String linkProvincia, String linkComune) {
        for (SingleAd sa : sas) {

            sa.setStartProcessing(new Date());
            try {
                ReAds reAds = new ReAds();
                HashMap<String, Object> prps = new HashMap<>();

                reAds.setLink(sa.getLink()); // link
                prps.put("link", sa.getLink());

                String idAd = sa.getLink().replaceAll("[^0-9]", "");
                reAds.setIdAd(idAd); //EXAMPLE --- https://www.immobiliare.it/annunci/65530776/
                prps.put("idAd", idAd);

                String provincia = linkProvincia.substring(linkProvincia.indexOf("vendita-case/") + 13, linkProvincia.indexOf("-provincia"));
                reAds.setProvincia(provincia);
                prps.put("provincia", provincia);

                String comune = linkComune.substring(linkComune.indexOf("vendita-case/") + 13, linkComune.length() - 1);
                reAds.setComune(comune);
                prps.put("comune", comune);

                Document doc = Jsoup.connect(sa.getLink()).get();

                Elements elements = doc.getElementsByClass(IM_TITLE_BLOCK_TITLE); //name
                elements.stream().findFirst().ifPresent(element -> reAds.setName(element.text()));

                elements = doc.getElementsByClass(IM_LOCATION); //locationsLevels
                reAds.setLocationLevels(new HashSet<>());
                elements.stream().forEach(element -> {
                    reAds.getLocationLevels().add(element.text());
                });

                elements = doc.getElementsByClass(IM_MAINFEATURES_TITLE); //price
                elements.stream().findFirst().ifPresent(element -> {
                    reAds.setPrice(element.text());
                    prps.put("price", element.text());
                });

                elements = doc.getElementsByClass(IM_FEATURES_VALUE); //surface
                elements.stream().filter(el -> el.text().contains("m²")).findFirst()
                        .ifPresent(el -> {
                            reAds.setSurface(el.text().replaceAll("[^0-9]", ""));
                            prps.put("surface", el.text().replaceAll("[^0-9]", ""));
                        }); //get(1).text()

                String toSearch = "";
                for (String ll : reAds.getLocationLevels()) {
                    toSearch = toSearch.concat(ll.replaceAll(" ", "%20")).concat("%20");
                }

                String req = GEOCODE_LINK.replace("{request}", toSearch);
                String content = connectionService.getJsonFromHttpRequest(req, RequestMethod.GET);
                /*while (content == null) {
                    LOGGER.info("Retry geocoding for {}", req);
                    content = connectionService.getJsonFromHttpRequest(req, RequestMethod.GET);
                }*/
                LOGGER.debug("Request result: {}", content);

                ObjectMapper mapper = new ObjectMapper();
                try {
                    if (content != null) {
                        HashMap map = mapper.readValue(content, HashMap.class);
                        if (map.get("features") != null) {
                            List<Object> listFeatures = (List<Object>) map.get("features");
                            if (listFeatures.size() > 0) {
                                HashMap featureMap = (HashMap) listFeatures.get(0);
                                LinkedHashMap geometry = (LinkedHashMap) featureMap.get("geometry");
                                ArrayList<Double> coordinates = (ArrayList<Double>) geometry.get("coordinates");
                                reAds.setLongitude(coordinates.get(0));
                                reAds.setLatitude(coordinates.get(1));
                                Point point = Point.fromLngLat(coordinates.get(0), coordinates.get(1));
                                Feature feature = Feature.fromGeometry(point);
                                fillPropertiesInFeatures(feature, prps);
                                reAds.setPoint(feature);
                            } else {
                                LOGGER.warn("Result blank for req {}", req);
                            }
                        }
                        LOGGER.info("Geocode completed for ad: {}", reAds.getLink());
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.error("Error during JsonProcessingException: {}", e.getMessage());
                }

                reAdsRepository.save(reAds);
                LOGGER.info("Saved reAds with link {}", sa.getLink());

            } catch (HttpStatusException e) {
                LOGGER.error("Page {} not exist", sa.getLink(), e);
                this.reAdsRepository.deleteByLink(sa.getLink());
                sa.setExpired(true);
            } catch (IOException e) {
                LOGGER.error("Error during get page {}", sa.getLink(), e);
            } catch (Exception e) {
                LOGGER.error("Generic error during parse page {}", sa.getLink(), e);
            }
            sa.setEndProcessing(new Date());
            sa.setProcessed(true);

        }
        //return sas;
    }

    private void updateProvinciaComune(ProvinciaImmo pi, ComuneImmo ci) {
        ci.setEndProcessing(new Date());
        ci.setProcessed(true);
        provinciaImmoRepository.save(pi);
        LOGGER.info("<<<< Completed comune immo {} >>>>", ci.getLink());
        pi.setEndProcessing(new Date());
        pi.setProcessed(true);
        provinciaImmoRepository.save(pi);
        LOGGER.info("++++ Completed single chunk for provincia immo {} ++++", pi.getLink());
    }

    private void resetProcessing() {
        this.provinciaImmoRepository.findAll().forEach(pi -> {
            pi.setProcessed(false);
            pi.getComuneImmos().forEach(comuneImmo -> {
                comuneImmo.setProcessed(false);
                comuneImmo.getSingleAds().forEach(singleAd -> {
                    singleAd.setProcessed(false);
                });
            });
            this.provinciaImmoRepository.save(pi);
            LOGGER.info("Resetted successfully provincia {}", pi.getLink());
        });
    }

    private void fillPropertiesInFeatures(Feature feature, Map<String, Object> properties) {
        properties.keySet().forEach(k -> {
            if (properties.get(k) instanceof Number) {
                feature.addNumberProperty(k, (Number) properties.get(k));
            } else if (properties.get(k) instanceof Double) {
                feature.addNumberProperty(k, (Double) properties.get(k));
            } else if (properties.get(k) instanceof Boolean) {
                feature.addBooleanProperty(k, (Boolean) properties.get(k));
            } else {
                feature.addStringProperty(k, (String) properties.get(k));
            }
        });
    }

}
