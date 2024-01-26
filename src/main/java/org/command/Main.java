package org.command;

import com.google.gson.Gson;

import javax.swing.*;
import java.io.*;
import java.util.LinkedList;
import java.util.Random;

public class Main {
    static Gson gson = new Gson();
    static File file;
    static Random rand = new Random();
    static LinkedList<String> players = new LinkedList<>();
    static LinkedList<Game> games = new LinkedList<>();

    public static void main(String[] args) {
        JFileChooser dialog = new JFileChooser();
        int returnVal = dialog.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION){
            file = dialog.getSelectedFile().getAbsoluteFile();
        }

        InputJSON input = null;
        try{
            input = gson.fromJson(readJSON(file), InputJSON.class);
        } catch (IOException e){
            System.out.println(e);
            System.exit(404);
        }

        boolean newName = false;
        for (Game game : input.games){
            newName = false;
            if (!players.isEmpty()){
                for (int i = 0; i < players.size(); i++){
                    if (players.get(i).equals(game.player)){
                        break;
                    }
                    if (i == players.size() - 1) {
                        newName = true;
                        break;
                    }
                }
            } else {
                newName = true;
            }
            if (newName){
                players.add(game.player);
            }
        }

        System.out.println("Players:");
        for (String name : players){
            System.out.println(name);
        }

        games.addAll(input.games);

        Multiworld start = new Multiworld();
        start.name = "start";

        Multiworld end = new Multiworld();
        end.name = "end";

        LinkedList<Game> personsGames = new LinkedList<>();
        for (String name : players){
            personsGames.clear();
            getPersonsGames(name, personsGames);

            int randInt = rand.nextInt(0,personsGames.size());
            start.games.add(personsGames.get(randInt));
            games.remove(personsGames.get(randInt));
        }

        if (input.allInEnding){
            for (String name : players){
                personsGames.clear();
                getPersonsGames(name, personsGames);
                if (personsGames.isEmpty()){
                    continue;
                }

                int randInt = rand.nextInt(0,personsGames.size());
                end.games.add(personsGames.get(randInt));
                games.remove(personsGames.get(randInt));
            }
        }

        System.out.println(gson.toJson(start) + "\n");
        LinkedList<Multiworld> unconnected = new LinkedList<>();
        unconnected.add(start);
        OutputJSON output = new OutputJSON();

        LinkedList<PathActions> applicableActions = new LinkedList<>();
        int openGames = games.size();
        int count = 1;
        while (true){
            count++;
            applicableActions.clear();
            openGames = games.size()-input.gamesPerMidWorld;
            if (openGames >= input.gamesPerMidWorld*2){
                applicableActions.add(PathActions.SPLIT);
            }
            if (openGames >= input.gamesPerMidWorld && unconnected.size() > 1){
                applicableActions.add(PathActions.MERGE);
            }
            if (openGames >= input.gamesPerMidWorld){
                applicableActions.add(PathActions.CONTINUE);
            }
            if (openGames < input.gamesPerMidWorld){
                if (input.allInEnding){
                    connectWorlds(unconnected.get(0), "game"+count);
                    output.worlds.add(unconnected.get(0));
                    unconnected.remove(0);

                    unconnected.add(new Multiworld());
                    unconnected.get(unconnected.size()-1).name = "game"+count;
                    unconnected.get(unconnected.size()-1).games.addAll(games);
                }
                break;
            }

            switch (applicableActions.get(rand.nextInt(0, applicableActions.size()))){
                case CONTINUE -> {
                    connectWorlds(unconnected.get(0), "game"+count);
                    output.worlds.add(unconnected.get(0));
                    unconnected.remove(0);

                    unconnected.add(newWorld(count, input.gamesPerMidWorld));
                }
                case MERGE -> {
                    int otherWorld = rand.nextInt(0, unconnected.size());
                    connectWorlds(unconnected.get(0), "game"+count);
                    connectWorlds(unconnected.get(otherWorld), "game"+(count));
                    output.worlds.add(unconnected.get(0));
                    output.worlds.add(unconnected.get(otherWorld));
                    unconnected.remove(otherWorld);
                    unconnected.remove(0);

                    unconnected.add(newWorld(count, input.gamesPerMidWorld));
                }
                case SPLIT -> {
                    connectWorlds(unconnected.get(0), "game"+count);
                    connectWorlds(unconnected.get(0), "game"+(count+1));
                    output.worlds.add(unconnected.get(0));
                    unconnected.remove(0);

                    unconnected.add(newWorld(count, input.gamesPerMidWorld));

                    count++;

                    unconnected.add(newWorld(count, input.gamesPerMidWorld));
                }
            }
        }

        for (Multiworld multiworld : unconnected) {
            multiworld.connections.add("end");
        }
        output.worlds.addAll(unconnected);

        if (!input.allInEnding){
            end.games.addAll(games);
        }
        output.worlds.add(end);

        System.out.println(gson.toJson(output));
    }

    public static String readJSON(File input) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(input.getAbsoluteFile()));
        String line = br.readLine();

        while (line != null){
            sb.append(line);
            line = br.readLine();
        }

        br.close();
        return sb.toString();
    }

    public static void getPersonsGames(String person, LinkedList personsGames){
        for (Game game : games){
            if (person.equals(game.player)) {
                personsGames.add(game);
            }
        }
    }

    public static void connectWorlds(Multiworld source, String connection){
        source.connections.add(connection);
    }

    public static void fillWorld(Multiworld input, int gameCount){
        for (int i = 0; i < gameCount; i++){
            int choice = rand.nextInt(0, games.size());
            input.games.add(games.get(choice));
            games.remove(choice);
        }
    }

    public static Multiworld newWorld(int count, int gameCount){
        Multiworld output = new Multiworld();
        output.name = "game"+count;
        fillWorld(output, gameCount);
        return output;
    }
}