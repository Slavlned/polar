package com.slavlend.Vm;

import com.slavlend.Colors;
import com.slavlend.PolarLogger;
import lombok.Getter;
import lombok.Setter;

import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
Виртуальная машина ICE
 */
@SuppressWarnings("unused")
@Getter
public class IceVm {
    // стек объектов
    private final ThreadLocal<Stack<Object>> stack = new ThreadLocal<>();
    // асинхронный пул потоков
    private final ExecutorService asyncExecutor = Executors.newCachedThreadPool();
    // хранилище
    private final VmFrame<String, Object> variables = new VmFrame<>();
    private final VmFrame<String, VmFunction> functions = new VmFrame<>();
    private final VmFrame<String, VmClass> classes = new VmFrame<>();
    private final VmFrame<String, VmCoreFunction> coreFunctions = new VmFrame<>();
    // рэйс ошибок
    @Setter
    public static VmErrRaiser raiser = new VmErrRaiser();
    // логгер
    @Setter
    public static VmErrLogger logger;
    // адресс последнего вызова функции
    private final ThreadLocal<VmInAddr> lastCallAddr = new ThreadLocal<>();

    /**
     * Указание адресса последнего вызова функции
     */
    public void setLastCallAddress(VmInAddr addr) {
        lastCallAddr.set(addr);
    }

    public VmInAddr getLastCallAddr() {
        return lastCallAddr.get();
    }

    /**
     *
     * @param code - код для запуска
     *             кода в виртуальной машине
     */
    public void run(VmCode code, boolean isDebugMode)  {
        // запуск
        try {
            // выводим байткод (инструкции вм)
            if (isDebugMode) {
                printByteCode(code);
            }
            // дефайн переменных среды
            variables.set("__VM__", this);
            // запускаем бенчмарк
            VmBenchmark benchmark = new VmBenchmark();
            benchmark.start();
            // инициализация стека
            initStackForThread();
            // исполняем код
            try {
                for (VmInstr instr : code.getInstructions()) {
                    instr.run(this, variables);
                }
            } catch (VmException e) {
                if (e.getValue() != null) {
                    PolarLogger.polarLogger.error(e.getAddr(), e.getMessage(), e.getValue());
                } else {
                    PolarLogger.polarLogger.error(e.getAddr(), e.getMessage());
                }
            }
            // останавливаем бенчмарк и
            // выводим время исполнения
            System.out.println();
            System.out.println(
                    Colors.ANSI_BLUE + "🧊 Exec time: " + benchmark.end() + "ms, stack size: "
                            + stack.get().size() + "(" + stack.get().toString() + ")" + Colors.ANSI_RESET
            );
            // выключаем асинхронный пул потоков
            asyncExecutor.shutdown();
        } catch (VmException exception) {
            if (exception.getValue() != null) {
                logger.error(exception.getAddr(), exception.getMessage(), exception.getValue());
            } else {
                logger.error(exception.getAddr(), exception.getMessage());
            }
        } catch (RuntimeException exception) {
            logger.error(new VmInAddr(-1), "java exception (" + exception.getMessage() + ")", exception);
        }
    }

    /**
     * Инициализация стека под поток
     */
    public void initStackForThread() {
        stack.set(new Stack<>());
    }

    /**
     *
     * @param code - код для вывода
     *             в консоль
     */
    private void printByteCode(VmCode code) {
        System.out.println(Colors.ANSI_BLUE + "🛞 Bytecode:" + Colors.ANSI_RESET);
        printCode(code);
        System.out.println();
    }

    /**
     * Выводит инструкции
     * @param code - код для вывода
     *             в консоль
     */
    private void printCode(VmCode code) {
        // выводим классы
        for (VmClass clazz : classes.getValues().values()) {
            clazz.print();
        }
        // выводим функции
        for (VmFunction function : functions.getValues().values()) {
            function.print();
        }
        // выводим другие инструкции
        for (VmInstr instr : code.getInstructions()) {
            instr.print();
        }
    }

    /**
     * Пушит в стек объект
     * @param val - объект, для помещения в стек
     */
    public void push(Object val) {
        stack().push(val);
    }

    /**
     * Удаляет объект с верхушки стека, возвращая его
     * @return - отдаёт объект с верхушки стека
     */
    public Object pop(VmInAddr addr) {
        if (stack().empty()) {
            logger.error(addr, "stack is empty here.", new RuntimeException("EmptyStack"));
        }
        return stack().pop();
    }

    /**
     * Загружает переменную в стек
     * @param frame - фрейм, для поиска переменной
     * @param name - имя переменной для поиска
     */
    public void load(VmInAddr addr, VmFrame<String, Object> frame, String name) {
        stack().push(frame.lookup(addr, name));
    }

    /**
     * Получение стека текущего потока
     * @return - стэк
     */
    public Stack<Object> stack() {
        return getStack().get();
    }

    /**
     * Вызывает глобальную функцию
     * @param name - имя для вызова
     */
    public void callGlobal(VmInAddr addr, String name, boolean shouldPushResult)  {
        if (functions.getValues().containsKey(name)) {
            functions.lookup(addr, name).exec(this, shouldPushResult);
        } else if (variables.getValues().containsKey(name)){
            ((VmFunction)variables.lookup(addr, name)).exec(this, shouldPushResult);
        } else {
            Object res = coreFunctions.lookup(addr, name).exec(addr);
            if (res != null) {
                push(res);
            }
        }
    }

    /**
     * Функция ли это - из ядра
     * @param name - имя функции
     * @return да или нет
     */
    public boolean isCoreFunc(String name) {
        return coreFunctions.has(name);
    }
}
