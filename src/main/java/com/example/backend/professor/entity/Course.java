/**
 * Represents a course entity in the system.
 * Includes basic metadata like course name, code, academic year, and term (Fall, Winter, or Summer).
 * Courses serve as containers for assignments.
 */

package com.example.backend.professor.entity;

import jakarta.persistence.*;

@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String code;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private String term; // Fall, Winter, or Summer

    @Column(nullable = false)
    private boolean hidden = false;


    public Course() {}

    public Course(String name, String code, int year, String term) {
        this.name = name;
        this.code = code;
        this.year = year;
        this.term = term;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public int getYear() { return year; }

    public String getTerm() { return term; }

    public boolean isHidden() { return hidden; }

    public void setName(String name) {
        this.name = name;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setYear(int year) { this.year= year; }

    public void setTerm(String term) { this.term = term; }

    public void setHidden(boolean hidden) { this.hidden = hidden; }


}
