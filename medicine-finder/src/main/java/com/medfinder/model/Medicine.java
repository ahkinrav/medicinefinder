package com.medfinder.model;

import jakarta.persistence.*;

@Entity
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Active ingredient, so people can search "paracetamol" and find brand names. */
    private String genericName;

    public Medicine() {}

    public Medicine(String name, String genericName) {
        this.name = name;
        this.genericName = genericName;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }
}
