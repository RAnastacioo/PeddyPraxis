package pt.ipleiria.pp.model;

import java.util.ArrayList;

public class SingletonPPB {
    private static final SingletonPPB ourInstance = new SingletonPPB();

    public static SingletonPPB getInstance() {
        return ourInstance;
    }

    private ArrayList<Game> games = new ArrayList<>();


    public boolean isNightMode() {
        return nightMode;
    }

    public void setNightMode(boolean nightMode) {
        this.nightMode = nightMode;
    }

    boolean nightMode= false;

    private SingletonPPB() {

        Game game = new Game("Benvindo ao IPL", "PeddyPraxis ", "Anastácio", 1);
        games.add(game);
        Task task1 = new Task("O pátio", "tarefa1", 5,game.getId());
        Task task2 = new Task("Os Edifícios", "tarefa2",5,game.getId());
        Task task3 = new Task("A Biblioteca", "tarefa3", 5,game.getId());
        Task task4 = new Task("Descompressao", "tarefa4", 5,game.getId());
        Task task5 = new Task("Sala de Aula", "tarefa5", 5,game.getId());
        Task task6 = new Task("Melhor Curso", "tarefa6", 5,game.getId());
        task1.setOrder(game.getTasks().size() + 1);
        game.getTasks().add(task1);
        task2.setOrder(game.getTasks().size() + 1);
        game.getTasks().add(task2);
        task3.setOrder(game.getTasks().size() + 1);
        game.getTasks().add(task3);
        task4.setOrder(game.getTasks().size() + 1);
        game.getTasks().add(task4);
        task5.setOrder(game.getTasks().size() + 1);
        game.getTasks().add(task5);
        task6.setOrder(game.getTasks().size() + 1);
        game.getTasks().add(task6);

    }

    public void setGames(ArrayList<Game> games) {
        this.games = games;
    }
    public void resetGames(){
        for (Game g : games) {
            g.setTimeOver(false);
            g.resetTask();
        }
    }


    public ArrayList<Game> getGames() {
        return games;
    }
    public Game containsID(String id) {

        for (Game g : games) {
            if (g.getId().equals(id)) {
                return g;
            }
        }
        return null;
    }

    public Task containsID(String id, Game game) {

        for (int i = 0; i < game.getTasks().size(); i++) {
            if (game.getTasks().get(i).getId().equals(id)) {
                return game.getTasks().get(i);
            }

        }
        return null;
    }


    public Task containsIDTask(String id) {

        for (int i = 0; i < games.size(); i++) {

            for (int j = 0; j < games.get(i).getTasks().size(); j++) {

                if (games.get(i).getTasks().get(j).getId().equals(id)) {

                    return games.get(i).getTasks().get(j);
                }
            }
        }
        return null;
    }

}
