package com.slavlend;

import com.slavlend.Commands.Command;
import com.slavlend.Commands.InstallPkgCommand;
import com.slavlend.Commands.RunCommand;
import com.slavlend.Executor.Executor;
import com.slavlend.Executor.ExecutorSettings;
import com.slavlend.Parser.Parser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class App 
{

    // парсер
    public static Parser parser;

    // список комманд
    public static HashMap<String, Command> commandList = new HashMap() {{
       put("pkg", new InstallPkgCommand());
       put("run", new RunCommand());
    }};

    /*
    Точка входа в приложение
     */
    public static void main(String[] args) throws Exception {
        // заголовочек
        System.out.println("╭───────────────────╮");
        System.out.println("│ 🐻‍❄️ Polar v1.0.27     ");
        System.out.println("╰───────────────────╯");
        System.out.println();
        // комманды
        showCommandMenu();
    }

    /*
    Меню выбора команды
     */
    public static void showCommandMenu() throws IOException {
        System.out.println("🐸 Choose command:");
        System.out.println(" > run (script name)");
        System.out.println(" > pkg (git repo)");
        // аргументы
        String[] inputArgs = new Scanner(System.in).nextLine().split(" ");
        String[] commandArgs = Arrays.copyOfRange(inputArgs, 1, inputArgs.length);
        // выполняем команду
        if (inputArgs.length > 0 &&
                commandList.containsKey(inputArgs[0])) {
            commandList.get(inputArgs[0]).execute(commandArgs);
        }
        else {
            System.out.println("🍕 Invalid Command.");
            showCommandMenu();
        }
    }
}

