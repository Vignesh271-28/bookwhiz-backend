package com.example.BookWhiz.model.movie;

import jakarta.persistence.*;

@Entity
@Table(name = "movies")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String genre;
    private String language;
    private String format;
    private String duration;
    private String director;

    @Column(length = 1000)
    private String cast;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String releaseDate;
    private String rating;

    // Poster served by backend e.g. http://localhost:8080/uploads/posters/abc.jpg
    @Column(length = 512)
    private String posterUrl;

    // ── Getters ───────────────────────────────────────────────
    public Long   getId()          { return id;          }
    public String getTitle()       { return title;       }
    public String getGenre()       { return genre;       }
    public String getLanguage()    { return language;    }
    public String getFormat()      { return format;      }
    public String getDuration()    { return duration;    }
    public String getDirector()    { return director;    }
    public String getCast()        { return cast;        }
    public String getDescription() { return description; }
    public String getReleaseDate() { return releaseDate; }
    public String getRating()      { return rating;      }
    public String getPosterUrl()   { return posterUrl;   }

    // ── Setters ───────────────────────────────────────────────
    public void setId(Long id)               { this.id          = id;       }
    public void setTitle(String v)           { this.title       = v;        }
    public void setGenre(String v)           { this.genre       = v;        }
    public void setLanguage(String v)        { this.language    = v;        }
    public void setFormat(String v)          { this.format      = v;        }
    public void setDuration(String v)        { this.duration    = v;        }
    public void setDirector(String v)        { this.director    = v;        }
    public void setCast(String v)            { this.cast        = v;        }
    public void setDescription(String v)     { this.description = v;        }
    public void setReleaseDate(String v)     { this.releaseDate = v;        }
    public void setRating(String v)          { this.rating      = v;        }
    public void setPosterUrl(String v)       { this.posterUrl   = v;        }
}