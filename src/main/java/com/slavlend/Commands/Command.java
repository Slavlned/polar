package com.slavlend.Commands;

import java.io.IOException;

/*
Обработчик комманды
 */
public interface Command {
    abstract void execute(String[] args) throws IOException;
}
