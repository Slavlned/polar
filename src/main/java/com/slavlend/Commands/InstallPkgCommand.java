package com.slavlend.Commands;

import com.slavlend.App;
import com.slavlend.Polar.Logger.PolarLogger;
import com.slavlend.Commands.Repo.RepoDownloader;

import java.io.IOException;

/*
Комманда установки пакета
 */
public class InstallPkgCommand implements Command {
    @Override
    public void execute(String[] args) throws IOException {
        // проверяем на колличество аргументов
        if (args.length != 1) {
            System.out.println(
                    "🛸 Error! Invalid args amount. (Expected:1,Founded:"
                            +String.valueOf(args.length) + ")"
            );
            return;
        }
        // имя пакета
        String gitPkg = args[0];
        // скачиваем пакет
        RepoDownloader.download(args[0], PolarLogger.getPolarPath() + "\\temp.rar");
        // показываем меню
        App.showCommandMenu();
    }
}
