package com.slavlend.VM.Instructions;

import com.slavlend.Parser.Operator;
import com.slavlend.VM.*;

/*
Соеденяет два кондишена
 */
public class VmInstrComputeConds implements VmInstr {
    // оператор
    private final Operator operator;

    public VmInstrComputeConds(Operator operator) {
        this.operator = operator;
    }

    @Override
    public void run(IceVm vm, VmFrame<Object> frame) {
        Object f = vm.pop();
        Object s = vm.pop();
        vm.push(equal(f, s));
    }
    public boolean equal(Object l, Object r) {
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
            throw new RuntimeException("invalid comparable types: " + l + ", " + r);
        }
    }

    @Override
    public String toString() {
        return "CCONDS(" + operator.operator + ")";
    }

    @Override
    public void print() {
        System.out.println(this);
    }
}
