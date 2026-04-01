package com.example.BookWhiz.model.venue;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "venue")
@Data
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String city;
    private String area;
    private String address;
    private Integer totalSeats;
    private Double longitude;
    private Double latitude;



    // Partner who owns this venue (null = system venue)
    @Column(name = "owner_email")
    private String ownerEmail;

    // Screen/hall name within the venue
    @Column(name = "screen_name")
    private String screenName;

    // Comma-separated amenities (e.g. "Dolby,4DX,Parking")
    @Column(columnDefinition = "TEXT")
    private String amenities;
}


