package com.slavlend.Vm.Instructions;

import com.slavlend.Parser.Operator;
import com.slavlend.Vm.*;
import lombok.Getter;

/*
Кодишен VM
 */
@SuppressWarnings("ConstantValue")
@Getter
public class VmInstrCondOperator implements VmInstr {
    // адресс
    private final VmInAddr addr;
    // оператор
    private final Operator operator;

    public VmInstrCondOperator(VmInAddr addr, Operator operator) {
        this.addr = addr;
        this.operator = operator;
    }

    public static <T> T cast(Class<T> clazz, Object o) {
        return clazz.cast(o);
    }

    @Override
    public void run(IceVm vm, VmFrame<String, Object> frame) {
        Object r = vm.pop(addr);
        Object l = vm.pop(addr);
        switch (operator.operator) {
            case "==" -> vm.push(equal(l, r));
            case "!=" -> vm.push(!equal(l, r));
            case "<" -> {
                if (l instanceof Float && r instanceof Float) {
                    vm.push((float) l < (float) r);
                } else {
                    throw new VmException(addr, "Not a number.", (l instanceof Number ? r : l).toString());
                }
            }
            case ">" -> {
                if (l instanceof Float && r instanceof Float) {
                    vm.push((float) l > (float) r);
                } else {
                    throw new VmException(addr, "Not a number.", (l instanceof Number ? r : l).toString());
                }
            }
            case "<=" -> {
                if (l instanceof Float && r instanceof Float) {
                    vm.push((float) l <= (float) r);
                } else {
                    throw new VmException(addr, "Not a number.", (l instanceof Number ? l : r).toString());
                }
            }
            case ">=" -> {
                if (l instanceof Float && r instanceof Float) {
                    vm.push((float) l >= (float) r);
                } else {
                    throw new VmException(addr, "Not a number.", (l instanceof Number ? l : r).toString());
                }
            }
            default -> throw new VmException(addr, "invalid operator!", operator.operator);
        }
    }

    // сравнивает два объекта
    public static boolean equal(Object l, Object r) {
        if (l instanceof String && r instanceof String) {
            return ((String)l).equals(((String)r));
        }
        else if (l == null && r != null) {
            return false;
        }
        else if (l != null && r == null) {
            return false;
        }
        else if (l == null && r == null) {
            return true;
        }
        else if (l instanceof VmObj && r instanceof VmObj) {
            return (((VmObj)l) == ((VmObj)r));
        }
        else if (l instanceof Boolean && r instanceof Boolean) {
            return (((boolean)l) == ((boolean)r));
        }
        else if (l instanceof Float && r instanceof Float) {
            return (((float) l) == ((float) r));
        }
        else if (l instanceof VmClass && r instanceof VmClass) {
            return (((VmClass)l) == ((VmClass)r));
        }
        else {
            // throw new RuntimeException("invalid comparables: " + l + ", " + r);
            return false;
        }
    }

    @Override
    public String toString() {
        return "COP(" + operator.operator + ")";
    }

    @Override
    public void print() {
        System.out.println(this);
    }
}
