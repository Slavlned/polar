package com.slavlend.Polar.Logger;

import com.slavlend.Exceptions.PolarException;
import com.slavlend.Polar.StackHistoryWriter;
import com.slavlend.Colors;
import com.slavlend.Parser.Address;

public class PolarLogger {
    /*
    Крашит выполнение, выводя сообщение
    об ошибке вместе с линией ошибки.
     */
    public static void exception(String message, Address line) {
        // исключение
        throw new PolarException(message, line.getLine(), null);
    }

    /*
    Крашит выполнение, выводя сообщение
    об ошибке вместе с линией ошибки а также стак трейсом
    джава функции.
     */
    public static void exception(String message, Address line, StackTraceElement[] stackTrace) {
        // исключение
        throw new PolarException(message, line.getLine(), stackTrace);
    }

    /*
    Выводит сообщение пользовательского варнинга
    вместе с линией варнинга, не останавливая(краша) выполнение программы.
     */
    public static void warning(String message, int line) {
        // warning
        System.out.println(Colors.ANSI_YELLOW +"╭──────────────────────────╮");
        System.out.println("│ ⚠️ Warning at: " + line);
        System.out.println("│ ☁️ Message: " + message);
        System.out.println("╰──────────────────────────╯" + Colors.ANSI_RESET);
    }

    /*
    Вывод ошибки
     */
    public static void printError(PolarException e) {
        // исключение
        System.out.println(Colors.ANSI_RED + "╭──────────────────────────╮");
        System.out.println("│ 📔 Line: " + e.getLine());
        System.out.println("│ 📕 Error: " + e.getError());
        System.out.println("│ ");
        System.out.println("│ 📑 Stack Trace: ");
        StackHistoryWriter.getInstance().printStackTrace();
        System.out.println("│ ");
        System.out.println("│ 📃 Java stack: ");
        // выводим java стак-трейс
        if (e.getStackTrace() == null) {
            printStackTrace(Thread.currentThread().getStackTrace());
        }
        else {
            printStackTrace(e.getStackTrace());
        }
        System.out.println("╰──────────────────────────╯" + Colors.ANSI_RESET);
        // выход с кодом ошибки
        System.exit(1);
    }

    // вывод java стак-трейса этого потока.
    public static void printStackTrace(StackTraceElement[] stackTraceElements) {
        for (StackTraceElement element : stackTraceElements) {
            System.out.println("| " + element);
        }
    }

    // получение пути к языку
    public static String getPolarPath() {
        // return System.getenv("POLAR_HOME");
        return "E:\\polar-lang\\language\\src\\main\\resources\\PolarDirectory\\pkgs";
    }
}
