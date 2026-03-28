/**
 * Data Transfer Object for representing an error type and its frequency/count.
 *
 * This class is primarily used to send error distribution data from the backend
 * to the frontend for visualization in charts such as pie charts or bar graphs.
 */

package com.example.backend.professor.dto;

public class ErrorTypeDTO {
    private String name;
    private int value;
    public ErrorTypeDTO(String name, int value) { this.name = name; this.value = value; }
    public String getName() { return name; }
    public int getValue() { return value; }
}