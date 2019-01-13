package pt.ipleiria.pp.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class Task implements Serializable {

    private String id;
    private String idGame;
    private int order;
    private String title;
    private String description;
    private int value;
    private ArrayList<String> ValidRule = new ArrayList<>();
    private boolean taskComplete =false;


    public Task(String title, String description, int value, String idGame) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.description = description;
        this.value = value;
        this.idGame=idGame;

    }

    public String getIdGame() {
        return idGame;
    }


    public boolean isTaskComplete() {
        return taskComplete;
    }

    public void setTaskComplete(boolean taskComplete) {
        this.taskComplete = taskComplete;
    }

    public String getId() {
        return id;
    }

    public int getOrder() {
        return order;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getValue() {
        return value;
    }

    public ArrayList<String> getValidRule() {
        return ValidRule;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public void setValidRule(ArrayList<String> validRule) {
        ValidRule = validRule;
    }
}
