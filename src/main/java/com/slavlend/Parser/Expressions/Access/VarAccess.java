package com.slavlend.Parser.Expressions.Access;

import com.slavlend.Compiler.Compiler;
import com.slavlend.Parser.Address;
import com.slavlend.Vm.Instructions.VmInstrLoad;

/*
Акссесс к функции
 */
public class VarAccess implements Access {
    // адресс
    private final Address address;
    // следующий
    public Access next;
    // имя функции
    public final String varName;

    // конструктор
    public VarAccess(Address address, Access next, String varName) {
        this.address = address;
        this.next = next;
        this.varName = varName;
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

    @Override
    public void compile(boolean hasPrevious, boolean isStatement) {
        boolean shouldPushResult = hasNext() || !isStatement;
        if (shouldPushResult) {
            Compiler.code.visitInstr(new VmInstrLoad(address.convert(), varName, hasPrevious));
        }
        if (hasNext()) {
            getNext().compile(true, isStatement);
        }
    }

    @Override
    public Access copy() {
        if (next != null) {
            return new VarAccess(address, next.copy(), varName);
        } else {
            return new VarAccess(address, null, varName);
        }
    }

    // в строку

    @Override
    public String toString() {
        return "VarAccess{" +
                "address=" + address +
                ", next=" + next +
                ", varName='" + varName + '\'' +
                '}';
    }
}
