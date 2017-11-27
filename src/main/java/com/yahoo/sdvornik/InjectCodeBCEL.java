package com.yahoo.sdvornik;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
import java.io.IOException;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.*;

public class InjectCodeBCEL {

  static public void main(String args[]) {
    //Get class to modify from program argument
    JavaClass mod = null;
    String methodName = (args.length >= 2) ? args[1] : "";
    int loopsize = (args.length >= 3) ? Integer.parseInt(args[2]) : 1;
    try {
      mod = Repository.lookupClass(args[0]);
    }
    catch (Exception e) {
      System.err.println("Could not get class " + args[0]);
      return;
    }

    //Create a generic class to modify
    ClassGen modClass = new ClassGen(mod);
    //Create a generic constantpool to modify
    ConstantPoolGen cp = modClass.getConstantPool();
    boolean methodEdited = false;

    Method[] methods = mod.getMethods();
    for (int i = 0; i < methods.length; i++) {
      if (methods[i].getName().equals(methodName)) {

        System.out.println("Method: " + methods[i]);
        // System.out.println("before:\n" + methods[i].getCode());
        modClass.removeMethod(methods[i]);
        Method newMethod = insertCodeInMethod(mod, methods[i], cp, loopsize);
        // System.out.println("after:\n" + newMethod.getCode());
        modClass.addMethod(newMethod);

        methodEdited = true;
      }
    }
    if (methodEdited) {
      modClass.update();
      try {
        //Write modified class
        JavaClass newClass = modClass.getJavaClass();
        String classname = args[0].replace(".","/");
        newClass.dump(classname + ".class");
        System.out.println("Class " + classname + " modified");
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static Method insertCodeInMethod(JavaClass mod, Method method, ConstantPoolGen cp, int loopsize) {
    MethodGen mg = new MethodGen(method, mod.getClassName(), cp);

    InstructionList il = mg.getInstructionList();
    InstructionHandle ihs = il.getStart();
    InstructionList ils = new InstructionList();
    InstructionFactory f = new InstructionFactory(cp);

    String CLASS_NAME = mod.getClassName();

    int ARRAY_SIZE = 100;
    int LOOP_SIZE = loopsize;

    int INCREASE_ID = mg.isStatic() ? 0 : 1; // if not static, this has position 0 on the stack
    Type[] types = mg.getArgumentTypes();
    // Increase the stack location(?) so they don't collide with the methods parameters.
    for (int i = 0; i < types.length; i++) {
      INCREASE_ID += types[i].getSize();
    }

    int VAR_ARRAY = 0 + INCREASE_ID;
    int VAR_I = 1 + INCREASE_ID;
    int VAR_II = 2 + INCREASE_ID;
    int VAR_I_MIN_1 = 3 + INCREASE_ID;
    int VAR_I_MIN_2 = 4 + INCREASE_ID;
    int VAR_SUM = 5 + INCREASE_ID;
    int VAR_JUMPTO = 6 + INCREASE_ID;

    // init array
    ils.append(new PUSH(cp, ARRAY_SIZE));
    ils.append(new NEWARRAY(Type.INT));
    ils.append(new ASTORE(VAR_ARRAY));

    // create iterator = 0 for while
    ils.append(new PUSH(cp, 0));
    ils.append(new ISTORE(VAR_I));

    // Main while loop:
    InstructionHandle beforeWhile = ils.append(new ILOAD(VAR_I));
    ils.append(new PUSH(cp, LOOP_SIZE));
    // While condition:
    BranchHandle whileCondition = ils.append(new IF_ICMPLT(null)); // if (VAR_I < LOOP_SIZE): jump to "whileBody"
    BranchHandle whileConditionFalseGoto = ils.append(new GOTO(null)); // if not: jump to "afterWhile"

    // While body:
    InstructionHandle whileBody = ils.append(new ILOAD(VAR_I));
    ils.append(new PUSH(cp, ARRAY_SIZE));
    ils.append(new IREM());
    ils.append(new ISTORE(VAR_II)); // create int ii = i % ARRAY_SIZE;

    // if (i == 0)
    ils.append(new ILOAD(VAR_I));
    ils.append(new PUSH(cp, 0));
    BranchHandle ifIteratorIs0 = ils.append(new IF_ICMPEQ(null));
    BranchHandle ifIteratorIs0FalseGoto = ils.append(new GOTO(null));
    // If true body
    InstructionHandle ifIteratorIs0Body = ils.append(new ALOAD(VAR_ARRAY));
    ils.append(new ILOAD(VAR_I));
    ils.append(new PUSH(cp, 0));
    ils.append(new IASTORE());
    BranchHandle ifIteratorIs0Done = ils.append(new GOTO(null));

    // "else" if (i != 1)
    InstructionHandle beginIfIteratorIsNot1 = ils.append(new ILOAD(VAR_I));
    ils.append(new PUSH(cp, 1));
    BranchHandle ifIteratorIsNot1 = ils.append(new IF_ICMPNE(null));
    // false: else: so in this case: if (!(i != 1)):
    ils.append(new ALOAD(VAR_ARRAY));
    ils.append(new ILOAD(VAR_I));
    ils.append(new PUSH(cp, 1));
    ils.append(new IASTORE());
    // done, go to i++;
    BranchHandle ifIteratorIsNot1FalseGoto = ils.append(new GOTO(null));

    // If true body (so if i != 1)..
    // create variable VAR_I_MIN_1 for array index (i-1)
    InstructionHandle ifIteratorIsNot1Body = ils.append(new ILOAD(VAR_I));
    ils.append(new PUSH(cp, 1));
    ils.append(new ISUB());
    ils.append(new PUSH(cp, ARRAY_SIZE));
    ils.append(new IREM());
    ils.append(new ISTORE(VAR_I_MIN_1)); // create int i_min_1 = (i - 1) % ARRAY_SIZE;
    // create variable VAR_I_MIN_2 for array index (i-2)
    ils.append(new ILOAD(VAR_I));
    ils.append(new PUSH(cp, 2));
    ils.append(new ISUB());
    ils.append(new PUSH(cp, ARRAY_SIZE));
    ils.append(new IREM());
    ils.append(new ISTORE(VAR_I_MIN_2)); // create int i_min_2 = (i - 2) % ARRAY_SIZE;
    // load the array values:
    ils.append(new ALOAD(VAR_ARRAY));
    ils.append(new ILOAD(VAR_I_MIN_1));
    ils.append(new IALOAD());
    ils.append(new ALOAD(VAR_ARRAY));
    ils.append(new ILOAD(VAR_I_MIN_2));
    ils.append(new IALOAD());
    // add the two values, and save them
    ils.append(new IADD());
    ils.append(new ISTORE(VAR_SUM));
    // add the new calculated number to the array
    ils.append(new ALOAD(VAR_ARRAY));
    ils.append(new ILOAD(VAR_II));
    ils.append(new ILOAD(VAR_SUM));
    ils.append(new IASTORE());
    // Done; go to i++;
    BranchHandle ifIteratorIsNot1Done = ils.append(new GOTO(null));

    // Increment i with 1
    InstructionHandle generalIfDoneGoto = ils.append(new IINC(VAR_I,1));

    // Goto that whil restart this loop:
    BranchHandle whileGotoBegin = ils.append(new GOTO(null)); // jumps to "beforeWhile"

    // We need something to jump to when done with the outer loop.
    InstructionHandle afterWhile = ils.append(new PUSH(cp, 0));
    ils.append(new ISTORE(VAR_JUMPTO));

    // While targets:
    whileCondition.setTarget(whileBody);
    whileConditionFalseGoto.setTarget(afterWhile);
    whileGotoBegin.setTarget(beforeWhile);
    // if (i == 0)
    ifIteratorIs0.setTarget(ifIteratorIs0Body);
    ifIteratorIs0FalseGoto.setTarget(beginIfIteratorIsNot1);
    ifIteratorIs0Done.setTarget(generalIfDoneGoto);
    // if (i == 1)
    ifIteratorIsNot1.setTarget(ifIteratorIsNot1Body);
    ifIteratorIsNot1FalseGoto.setTarget(generalIfDoneGoto);
    ifIteratorIsNot1Done.setTarget(generalIfDoneGoto);

    InstructionHandle ihss = il.insert(ihs,ils);
    il.redirectBranches(ihs, ihss);
    il.update();

    mg.setMaxStack();
    mg.setMaxLocals();
    mg.update();
    return mg.getMethod();
  }
}