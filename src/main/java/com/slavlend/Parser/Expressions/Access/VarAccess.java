package com.slavlend.Parser.Expressions.Access;

import com.slavlend.Polar.PolarClass;
import com.slavlend.Polar.PolarObject;
import com.slavlend.Polar.PolarValue;
import com.slavlend.Polar.Reflected;
import com.slavlend.Polar.Stack.Classes;
import com.slavlend.Polar.Stack.Storage;
import com.slavlend.Logger.PolarLogger;
import com.slavlend.Parser.Address;

import java.lang.reflect.Field;

/*
Акссесс к функции
 */
public class VarAccess implements Access {
    // следующий
    public Access next;
    // имя функции
    public final String varName;
    // аддресс
    private final Address address;

    // конструктор
    public VarAccess(Address address, Access next, String varName) {
        this.address = address;
        this.next = next;
        this.varName = varName;
    }

    // акссес
    @Override
    public PolarValue access(PolarValue previous) {
        // если нет предыдущего
        if (previous == null) {
            // получаем переменную
            PolarValue res = null;
            // если есть переменная
            if (Storage.getInstance().has(varName)) {
                res = Storage.getInstance().get(address, varName);
            }
            else {
                if (Classes.getInstance().hasClass(varName)) {
                    res = new PolarValue(Classes.getInstance().lookupClass(address, varName));
                }
                else {
                    PolarLogger.exception("Not Found: " + varName, address);
                }
            }

            // если нет следующего
            if (next == null) {
                return res;
            }
            else {
                return next.access(res);
            }
        }
        // если есть
        else {
            // если предыдущий объект
            if (previous.isObject()) {
                // получаем переменную
                PolarObject v = previous.asObject();
                // вызываем
                PolarValue res = v.getClassValues().get(varName);

                // если нет следующего
                if (next == null) {
                    return res;
                } else {
                    return next.access(res);
                }
            }
            // если предыдущий рефлектед
            else if (previous.isReflected()) {
                // получаем переменную
                Reflected r = previous.asReflected();
                PolarValue res = null;
                // получаем
                try {
                    // получаем значение java-переменной
                    Field field = r.getClazz().getField(varName);
                    field.setAccessible(true);
                    Object result = field.get(r.getReflectedObject());
                    // в зависимости от типа создаем переменную
                    if (result instanceof Integer ||
                        result instanceof Long ||
                        result instanceof Float ||
                        result instanceof Double) {
                        res = new PolarValue(result);
                    } else if (result instanceof String || result instanceof Boolean) {
                        res = new PolarValue(result);
                    } else {
                        res = new PolarValue(new Reflected(address, result.getClass(), result));
                    }
                } catch (IllegalAccessException e) {
                    PolarLogger.exception("Illegal Access Exception (Java): " + e.getMessage(), address);
                } catch (NoSuchFieldException e) {
                    PolarLogger.exception("No Such Field Exception (Java): " + e.getMessage(), address);
                }

                // если нет следующего
                if (next == null) {
                    return res;
                } else {
                    return next.access(res);
                }
            }
            // если предыдущий класс
            else {
                // получаем класс
                PolarClass v = previous.asClass();
                // получаем переменную
                PolarValue res = v.lookupModuleValues().get(varName);

                // если нет следующего
                if (next == null) {
                    return res;
                } else {
                    return next.access(res);
                }
            }
        }
    }

    @Override
    public void setNext(Access access) {
        if (next != null) {
            next.setNext(access);
        }
        else {
            next = access;
        }
    }

    @Override
    public void setLast(Access access) {
        if (next.hasNext()) {
            next.setLast(access);
        }
        else {
            next = access;
        }
    }

    @Override
    public boolean hasNext() { return next != null; }

    @Override
    public Access getNext() { return next; }
}
