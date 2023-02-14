package it.ibee.scraperimmo.model;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document(collection = "reAds")
public class ReAds extends AbstractEntity {

    private String link;
    private String idAd;
    private String name;
    private Set<String> locationLevels;
    private String price;
    private String surface;
    private String comune;
    private String provincia;
    private Double longitude;
    private Double latitude;
    private Object point;

    @PersistenceConstructor
    public ReAds() {
        this.locationLevels = new HashSet<>();
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getIdAd() {
        return idAd;
    }

    public void setIdAd(String idAd) {
        this.idAd = idAd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getLocationLevels() {
        return locationLevels;
    }

    public void setLocationLevels(Set<String> locationLevels) {
        this.locationLevels = locationLevels;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getSurface() {
        return surface;
    }

    public void setSurface(String surface) {
        this.surface = surface;
    }

    public String getComune() {
        return comune;
    }

    public void setComune(String comune) {
        this.comune = comune;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Object getPoint() {
        return point;
    }

    public void setPoint(Object point) {
        this.point = point;
    }
}
