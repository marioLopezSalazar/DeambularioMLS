package com.iesaguadulce.deambulario.model.pojos;

import androidx.annotation.NonNull;

/**
 * POJO class representing a Frequently Asked Question (FAQ).*
 *
 * @author Mario López Salazar
 */
public class FaqItem {

    // --- OBJECT ATTRIBUTES ---

    /*
     * The question text.
     */
    private String question;

    /*
     * The answer text.
     */
    private String answer;

    /*
     * Indicates whether the answer is currently visible (expanded) or not.
     */
    private boolean expanded;


    // --- CONSTRUCTORS ---

    /**
     * Constructor for building a new FAQ item. By default, the item is created in a collapsed state.
     * @param question The text of the question.
     * @param answer   The text of the answer.
     */
    public FaqItem(@NonNull String question, @NonNull String answer) {
        this.question = question;
        this.answer = answer;
        this.expanded = false;
    }


    // --- GETTERS AND SETTERS ---

    /**
     * Gets the text of the question.
     * @return The question.
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Allows to set the text of the question.
     * @param question The question.
     */
    public void setQuestion(@NonNull String question) {
        this.question = question;
    }

    /**
     * Gets the text of the answer.
     * @return The answer.
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Allows to set the text of the answer.
     * @param answer The answer.
     */
    public void setAnswer(@NonNull String answer) {
        this.answer = answer;
    }

    /**
     * Checks if the FAQ item is expanded (answer is visible).
     * @return True if expanded, false otherwise.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Allows to set the expanded state of the FAQ item.
     * @param expanded True to expand, false to collapse.
     */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }
}