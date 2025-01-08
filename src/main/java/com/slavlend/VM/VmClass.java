package com.slavlend.VM;

import com.slavlend.Parser.Expressions.ArgumentExpression;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/*
Класс VM
 */
@Getter
public class VmClass implements VmInstrContainer {
    // имя класса
    private final String name;
    // функции
    private final VmFrame<VmFunction> functions = new VmFrame<>();
    // модульные значения
    private final VmFrame<Object> modValues = new VmFrame<>();
    // конструктор для класса
    private final ArrayList<ArgumentExpression> constructor;
    // модульные функции
    private final VmFrame<VmFunction> modFunctions = new VmFrame<VmFunction>();
    // пишется ли в модульные функции
    @Setter
    private boolean isModuleFunctionsWriting = false;

    // конструктор
    public VmClass(String name, ArrayList<ArgumentExpression> constructor) {
        this.name = name;
        this.constructor = constructor;
    }

    /**
     * Записывает инструкцию
     * @param instr - инструкция, в данном
     *              случае - функция
     */
    @Override
    public void visitInstr(VmInstr instr) {
        throw new RuntimeException("cannot visit instr: " + instr + " with class!");
    }

    /**
     * Выводит информацию о классе в консоль
     */
    public void print() {
        System.out.println("╭──────────class───────────╮");
        for (VmFunction instr : functions.getValues().values()) {
            instr.print();
        }
        System.out.println("╭───────────mod────────────╮");
        for (VmFunction instr : modFunctions.getValues().values()) {
            instr.print();
        }
        System.out.println("╰──────────────────────────╯");
        System.out.println("╰──────────────────────────╯");
    }
}
