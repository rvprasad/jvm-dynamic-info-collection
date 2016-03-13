/*
 * Copyright (c) 2016, Venkatesh-Prasad Ranganath
 *
 * BSD 3-clause License
 *
 * Author: Venkatesh-Prasad Ranganath (rvprasad)
 */
package sindu.instrumenter;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import sindu.instrumenter.Logger.Action;

public class LoggingInstrumentationHelper {

    static {
        try {
            OWNER = Logger.class.getName().replace(".", "/");
            LOG_STRING = Method.getMethod(Logger.class.getMethod("log",
                    new Class[]{String.class}));
            LOG_METHOD_ENTRY = Method.getMethod(Logger.class.getMethod(
                    "logMethodEntry", new Class[]{String.class}));
            LOG_METHOD_EXIT = Method.getMethod(Logger.class.getMethod(
                    "logMethodExit", new Class[]{String.class,
                        Character.TYPE}));
            LOG_ARGUMENT = Method.getMethod(Logger.class.getMethod(
                    "logArgument", new Class[]{Byte.TYPE,
                        String.class}));
            LOG_RETURN = Method.getMethod(Logger.class.getMethod(
                    "logReturn", new Class[]{String.class}));
            LOG_FIELD = Method.getMethod(Logger.class.getMethod(
                    "logField", new Class[]{Object.class,
                        String.class,
                        String.class,
                        Action.class}));
            LOG_ARRAY = Method.getMethod(Logger.class.getMethod(
                    "logArray", new Class[]{Object.class,
                        Integer.TYPE,
                        String.class,
                        Action.class}));
            LOG_EXCEPTION = Method.getMethod(
                    Logger.class.getMethod("logException",
                    new Class[]{ Throwable.class}));
        } catch (final NoSuchMethodException _ex) {
            throw new RuntimeException(_ex);
        } catch (final SecurityException _ex) {
            throw new RuntimeException(_ex);
        }
    }

    public static void emitLogMethodEntry(final MethodVisitor mv,
            final String methodId) {
        mv.visitLdcInsn(methodId);
        emitInvokeLog(mv, LOG_METHOD_ENTRY);
    }

    public static void emitLogMethodExit(final MethodVisitor mv,
            final String methodId, final String exitId) {
        mv.visitLdcInsn(methodId);
        mv.visitLdcInsn(exitId);
        emitInvokeLog(mv, LOG_METHOD_EXIT);
    }

    public static int emitLogArgument(final MethodVisitor mv,
            final int index, final Type argType) {
        mv.visitLdcInsn(Integer.valueOf(index));

        int _typeLength = 1;
        switch (argType.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.INT:
            case Type.SHORT:
                mv.visitVarInsn(Opcodes.ILOAD, index);
                break;
            case Type.LONG:
                mv.visitVarInsn(Opcodes.LLOAD, index);
                _typeLength++;
                break;
            case Type.FLOAT:
                mv.visitVarInsn(Opcodes.FLOAD, index);
                break;
            case Type.DOUBLE:
                mv.visitVarInsn(Opcodes.DLOAD, index);
                _typeLength++;
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                mv.visitVarInsn(Opcodes.ALOAD, index);
                break;
        }

        emitConvertToString(mv, argType);
        emitInvokeLog(mv, LOG_ARGUMENT);

        return _typeLength;
    }

    public static void emitLogReturn(final MethodVisitor mv,
        final Type returnType) {
        final int _retSort = returnType.getSort();
        if (_retSort != Type.VOID) {
            if (_retSort == Type.LONG || _retSort == Type.DOUBLE) {
                mv.visitInsn(Opcodes.DUP2);
            } else {
                mv.visitInsn(Opcodes.DUP);
            }
            emitConvertToString(mv, returnType);
            emitInvokeLog(mv, LOG_RETURN);
        }
    }

    public static void emitLogField(final MethodVisitor mv,
        final String fieldName, final Type fieldType, final Action action) {
        final int _fieldSort = fieldType.getSort();
        if (_fieldSort == Type.LONG || _fieldSort == Type.DOUBLE) {
            mv.visitInsn(Opcodes.DUP2_X1);
        } else {
            mv.visitInsn(Opcodes.DUP_X1);
        }

        emitConvertToString(mv, fieldType);
        mv.visitLdcInsn(fieldName);

        final Type _a = Type.getType(Action.class);
        final String _name = action.toString();
        mv.visitFieldInsn(Opcodes.GETSTATIC, _a.getInternalName(), _name,
                _a.getDescriptor());
        emitInvokeLog(mv, LOG_FIELD);
    }

    public static void emitLogArray(final MethodVisitor mv,
        final Action action) {        
        final Type _a = Type.getType(Action.class);
        final String _name = action.toString();
        mv.visitFieldInsn(Opcodes.GETSTATIC, _a.getInternalName(), _name, 
            _a.getDescriptor());
        emitInvokeLog(mv, LOG_ARRAY);
    }
    
    public static void emitSwapTwoWordsAndOneWord(final MethodVisitor mv,
        final Type tos) {
        if (tos.getSort() == Type.LONG
                || tos.getSort() == Type.DOUBLE) {
            mv.visitInsn(Opcodes.DUP2_X1);
            mv.visitInsn(Opcodes.POP2);
        } else {
            mv.visitInsn(Opcodes.SWAP);
        }
    }

    public static void emitSwapOneWordAndTwoWords(final MethodVisitor mv,
        final Type tos) {
        if (tos.getSort() == Type.LONG
                || tos.getSort() == Type.DOUBLE) {
            mv.visitInsn(Opcodes.DUP_X2);
            mv.visitInsn(Opcodes.POP);
        } else {
            mv.visitInsn(Opcodes.SWAP);
        }
    }

    public static void emitInvokeLog(final MethodVisitor mv,
        final Method method) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER,
            method.getName(), method.getDescriptor());
    }

    public static void emitConvertToString(final MethodVisitor mv,
        final Type type) {
        switch (type.getSort()) {
            case Type.ARRAY:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(Ljava/lang/Object;)Ljava/lang/String;");
                break;
            case Type.BOOLEAN:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(Z)Ljava/lang/String;");
                break;
            case Type.BYTE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(B)Ljava/lang/String;");
                break;
            case Type.CHAR:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(C)Ljava/lang/String;");
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(D)Ljava/lang/String;");
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(F)Ljava/lang/String;");
                break;
            case Type.INT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(I)Ljava/lang/String;");
                break;
            case Type.LONG:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(J)Ljava/lang/String;");
                break;
            case Type.SHORT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(S)Ljava/lang/String;");
                break;
            case Type.OBJECT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, OWNER, "toString",
                        "(Ljava/lang/Object;)Ljava/lang/String;");
                break;
            default:
                throw new RuntimeException("Unknown type"
                        + type.getInternalName());
        }
    }
    
    public static void emitLogException(final MethodVisitor mv) {
        mv.visitInsn(Opcodes.DUP);
        LoggingInstrumentationHelper.emitInvokeLog(mv,
                LoggingInstrumentationHelper.LOG_EXCEPTION);
    }
    
    private LoggingInstrumentationHelper() { }
    
    public static final String OWNER;
    public static final Method LOG_STRING;
    private static final Method LOG_METHOD_ENTRY;
    private static final Method LOG_METHOD_EXIT;
    private static final Method LOG_ARGUMENT;
    private static final Method LOG_RETURN;
    private static final Method LOG_FIELD;
    private static final Method LOG_ARRAY;
    private static final Method LOG_EXCEPTION;
}
