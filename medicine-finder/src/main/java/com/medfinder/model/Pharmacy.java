package com.medfinder.model;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
public class Pharmacy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;
    private String phone;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    /** If opensAt equals closesAt the pharmacy is treated as open 24 hours. */
    private LocalTime opensAt;
    private LocalTime closesAt;

    public Pharmacy() {}

    public Pharmacy(String name, String address, String phone, double latitude, double longitude,
                    LocalTime opensAt, LocalTime closesAt) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.opensAt = opensAt;
        this.closesAt = closesAt;
    }

    public boolean isOpenAt(LocalTime now) {
        if (opensAt == null || closesAt == null || opensAt.equals(closesAt)) return true;
        if (opensAt.isBefore(closesAt)) {
            return !now.isBefore(opensAt) && now.isBefore(closesAt);
        }
        return !now.isBefore(opensAt) || now.isBefore(closesAt); // overnight shift
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public LocalTime getOpensAt() { return opensAt; }
    public void setOpensAt(LocalTime opensAt) { this.opensAt = opensAt; }
    public LocalTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalTime closesAt) { this.closesAt = closesAt; }
}
