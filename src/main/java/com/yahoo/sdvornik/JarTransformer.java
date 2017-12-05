package com.yahoo.sdvornik;


import org.apache.bcel.Const;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.*;
import java.util.regex.Pattern;

import static java.lang.System.out;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
public class JarTransformer {

  private final String pathToInputJar;

  private final Path pathToOutputDirectory;

  private final Configuration conf;

  public JarTransformer(String pathToInputJar, Path pathToOutputDirectory, Configuration conf) {

    this.pathToInputJar = pathToInputJar;

    this.pathToOutputDirectory = pathToOutputDirectory;

    this.conf = conf;
  }

  public void extract() throws Exception {

    String inputJarName = Paths.get(pathToInputJar).getFileName().toString();

    String outputJarName = inputJarName.replace(".jar", "-mod.jar");

    String pathToOutputJar = pathToOutputDirectory.resolve(outputJarName).toString();

    JarFile jar = new JarFile(this.pathToInputJar);

    Manifest manifestOld = jar.getManifest();


    Manifest manifestNew = new Manifest();
    Attributes attrs = manifestNew.getMainAttributes();
    manifestOld.getMainAttributes().entrySet().forEach(entry -> {
      attrs.put(entry.getKey(), entry.getValue());
    });

    JarOutputStream jos = new JarOutputStream(new FileOutputStream(pathToOutputJar), manifestNew);

    Enumeration jarEntries = jar.entries();

    while (jarEntries.hasMoreElements()) {

      JarEntry inputJarEntry = (JarEntry) jarEntries.nextElement();

      InputStream is = jar.getInputStream(inputJarEntry);

      String jarEntryName = inputJarEntry.getName();

      // skip manifest
      if (jarEntryName.toLowerCase().startsWith("meta-inf/manifest")) continue;

      boolean modified = false;
      JavaClass jcMod = null;
      // process class
      if (jarEntryName.endsWith(".class")) {
        String clazz = jarEntryName.replace(".class", "").replace("/", ".");

        ClassParser parser = new ClassParser(is, clazz);

        JavaClass jc = parser.parse();

        ClassGen classGen = null;

        Method[] methodArr = null;

        for (String elm : jc.getInterfaceNames()) {

          switch (elm) {
            case "java.sql.PreparedStatement":
              for (Method m : jc.getMethods()) {
                switch (m.getName()) {
                  case "executeQuery":
                  case "executeUpdate":
                  case "execute":
                  case "addBatch":
                    modified = true;
                    classGen = new ClassGen(jc);
                    replaceMethod(classGen, m);
                    break;
                  default:
                    continue;
                }
              }

              break;

            case "java.sql.CallableStatement":
              modified = true;
              classGen = new ClassGen(jc);
              for (Method m : jc.getMethods()) {
                switch (m.getName()) {
                  case "executeQuery":
                  case "executeUpdate":
                  case "execute":
                  case "addBatch":
                    replaceMethod(classGen, m);
                    break;
                  default:
                    continue;
                }
              }
              break;

            case "java.sql.Connection":
              modified = true;
              classGen = new ClassGen(jc);
              for (Method m : jc.getMethods()) {
                switch (m.getName()) {
                  case "prepareStatement":
                  case "prepareCall":
                  case "nativeSQL":
                    replaceMethod(classGen, m);
                    break;
                  default:
                    continue;
                }
              }

              break;

            case "java.sql.Statement":
              modified = true;
              classGen = new ClassGen(jc);

              for (Method m : jc.getMethods()) {
                switch (m.getName()) {
                  case "executeQuery":
                  case "executeUpdate":
                  case "execute":
                  case "addBatch":
                    replaceMethod(classGen, m);
                    break;
                  default:
                    continue;
                }
              }

              break;

            case "java.sql.DatabaseMetaData":
              classGen = new ClassGen(jc);

              for (Method m : jc.getMethods()) {
                switch (m.getName()) {
                  case "getDatabaseMajorVersion":
                    if (conf.databaseMajorVersion != null) {
                      modified = true;
                      createGetDatabaseMajorVersion(classGen, m);
                    }
                    break;

                  case "getDatabaseProductVersion":
                    if (conf.databaseProductVersion != null) {
                      modified = true;
                      createGetDatabaseProductVersion(classGen, m);
                    }
                    break;
                  case "getDatabaseProductName":
                    if (conf.databaseProductName != null) {
                      modified = true;
                      createGetDatabaseProductName(classGen, m);
                    }
                    break;
                }
              }
              break;
          }
        }
        if (modified) {

          jcMod = classGen.getJavaClass();
        }
      }
      jos.putNextEntry(new JarEntry(inputJarEntry.getName()));

      if (!modified) {
        is = jar.getInputStream(inputJarEntry);
        byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while ((bytesRead = is.read(buffer)) != -1) {
          jos.write(buffer, 0, bytesRead);
        }
        is.close();
      } else {
        byte[] buffer = jcMod.getBytes();
        jos.write(buffer, 0, buffer.length);
      }
      jos.flush();
      jos.closeEntry();
    }
    // add Transformator in jar
    if (conf.matchers != null && conf.replacers != null) {

      JavaClass jcMod = modifyTransformator();
      jos.putNextEntry(new JarEntry("com/yahoo/sdvornik/Transformator.class"));
      byte[] buffer = jcMod.getBytes();
      jos.write(buffer, 0, buffer.length);
      jos.flush();
      jos.closeEntry();
    }
    jos.close();
  }

  private void replaceMethod(ClassGen classGen, Method method) {
    ConstantPoolGen cp = classGen.getConstantPool();
    String className = classGen.getClassName();

    MethodGen methodGen = new MethodGen(method, className, cp);

    InstructionList insertedIL = new InstructionList();

    InstructionList currentIL = methodGen.getInstructionList();

    insertedIL.append(new ALOAD(1));
    insertedIL.append(
      new INVOKESTATIC(
        cp.addMethodref(
          "com.yahoo.sdvornik.Transformator",
          "transform",
          "(Ljava/lang/String;)Ljava/lang/String;"
        )
      )
    );
    insertedIL.append(new ASTORE(1));

    Iterator instructionIterator = currentIL.iterator();
    InstructionHandle ih = (InstructionHandle) instructionIterator.next();
    currentIL.insert(ih, insertedIL);

    methodGen.setMaxStack();

    classGen.replaceMethod(method, methodGen.getMethod());

  }

  public JavaClass modifyTransformator() {

    ClassGen genClass = new ClassGen(
      "com.yahoo.sdvornik.Transformator",
      "java.lang.Object",
      "com/yahoo/sdvornik/Transformator.class",
      Const.ACC_PUBLIC,
      null
    );

    ConstantPoolGen cp = genClass.getConstantPool();

    int refToStrArr = cp.addClass(ObjectType.STRING);
    int refToPatternArr = cp.addClass(new ObjectType("java.util.regex.Pattern"));
    cp.addClass(new ObjectType("java.util.regex.Matcher"));
    cp.addClass(new ObjectType("java.lang.System"));
    cp.addClass(new ObjectType("java.io.PrintStream"));


    FieldGen patternFieldGen = new FieldGen(
      Const.ACC_STATIC,
      Type.getType("[Ljava/util/regex/Pattern;"),
      "patterns",
      cp
    );

    int patternsFieldRef = cp.addFieldref(
      genClass.getClassName(),
      "patterns",
      "[Ljava/util/regex/Pattern;"
    );

    FieldGen replacerFieldGen = new FieldGen(
      Const.ACC_STATIC,
      Type.getType("[Ljava/lang/String;"),
      "replacers",
      cp
    );

    int replacersFieldRef = cp.addFieldref(
      genClass.getClassName(),
      "replacers",
      "[Ljava/lang/String;"
    );

    int matcherMethodRef = cp.addMethodref(
      "java.util.regex.Pattern",
      "matcher",
      "(Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;"
    );


    int replacerMethodRef = cp.addMethodref(
      "java.util.regex.Matcher",
      "replaceAll",
      "(Ljava/lang/String;)Ljava/lang/String;"
    );

    int initRef = cp.addMethodref(
      "java.lang.Object",
      "<init>",
      "()V;"
    );

    int outFieldRef = cp.addFieldref(
      "java.lang.System",
      "out",
      "Ljava/io/PrintStream;"
    );

    int printlnMethodRef = cp.addMethodref(
      "java.io.PrintStream",
      "println",
      "(Ljava/lang/String;)V;"
    );

    // TODO Create clinit
    InstructionList clinitIL = new InstructionList();

    clinitIL.append(new PUSH(cp, conf.replacers.length));
    clinitIL.append(new ANEWARRAY(refToStrArr));

    for (int j = 0; j < conf.replacers.length; ++j) {
      clinitIL.append(new DUP());
      clinitIL.append(new PUSH(cp, j));
      clinitIL.append(new LDC(cp.addString(conf.replacers[j])));
      clinitIL.append(new AASTORE());
    }
    clinitIL.append(new PUTSTATIC(replacersFieldRef));

    clinitIL.append(new PUSH(cp, conf.matchers.length));
    clinitIL.append(new ANEWARRAY(refToPatternArr));

    for (int j = 0; j < conf.matchers.length; ++j) {
      clinitIL.append(new DUP());
      clinitIL.append(new PUSH(cp, j));
      clinitIL.append(new LDC(cp.addString(conf.matchers[j])));
      clinitIL.append(new ICONST(Pattern.CASE_INSENSITIVE));

      clinitIL.append(
        new INVOKESTATIC(
          cp.addMethodref(
            "java.util.regex.Pattern",
            "compile",
            "(Ljava/lang/String;I)Ljava/util/regex/Pattern;"
          )
        )
      );
      clinitIL.append(new AASTORE());
    }
    clinitIL.append(new PUTSTATIC(patternsFieldRef));
    clinitIL.append(new RETURN());

    MethodGen clinitMethodGen = new MethodGen(
      Const.ACC_STATIC,
      Type.VOID,
      Type.NO_ARGS,
      null,
      "<clinit>",
      genClass.getClassName(),
      clinitIL,
      cp
    );
    clinitMethodGen.setMaxLocals();
    clinitMethodGen.setMaxStack();

    // TODO CREATE init
    InstructionList initIL = new InstructionList();
    initIL.append(new ALOAD(0));
    initIL.append(new INVOKESPECIAL(initRef));

    MethodGen initMethodGen = new MethodGen(
      Const.ACC_PRIVATE,
      Type.VOID,
      Type.NO_ARGS,
      null,
      "<init>",
      genClass.getClassName(),
      initIL,
      cp
    );
    initMethodGen.setMaxLocals();
    initMethodGen.setMaxStack();



    // TODO CREATE transform
    InstructionList transformIL = new InstructionList();

    int LENGTH = 1;
    int COUNT = 2;

    //GETSTATIC com/yahoo/sdvornik/Transformator.patterns : [Ljava/util/regex/Pattern;
    transformIL.append(new GETSTATIC(patternsFieldRef));

    // ARRAYLENGTH
    transformIL.append(new ARRAYLENGTH());
    // ISTORE 1
    transformIL.append(new ISTORE(LENGTH));

/*
    transformIL.append(new GETSTATIC(outFieldRef));
    transformIL.append(new ALOAD(0));
    transformIL.append(new INVOKEVIRTUAL(printlnMethodRef));
*/
    // ICONST_0
    transformIL.append(new ICONST(0));
    // ISTORE 2
    transformIL.append(new ISTORE(COUNT));

    // ILOAD 2
    InstructionHandle ifHandle = transformIL.append(new ILOAD(COUNT));
    // ILOAD 1
    transformIL.append(new ILOAD(LENGTH));
    // IF_ICMPGE L3
    BranchHandle ifCheck = transformIL.append(new IF_ICMPGE(null));

    // GETSTATIC com/yahoo/sdvornik/Transformator.patterns : [Ljava/util/regex/Pattern;
    transformIL.append(new GETSTATIC(patternsFieldRef));
    // ILOAD 2
    transformIL.append(new ILOAD(COUNT));
    // AALOAD
    transformIL.append(new AALOAD());
    // ALOAD 0
    transformIL.append(new ALOAD(0));
    // INVOKEVIRTUAL java/util/regex/Pattern.matcher (Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;
    transformIL.append(new INVOKEVIRTUAL(matcherMethodRef));
    // GETSTATIC com/yahoo/sdvornik/Transformator.replacers : [Ljava/lang/String;
    transformIL.append(new GETSTATIC(replacersFieldRef));
    // ILOAD 2
    transformIL.append(new ILOAD(COUNT));
    // AALOAD
    transformIL.append(new AALOAD());
    // INVOKEVIRTUAL java/util/regex/Matcher.replaceAll (Ljava/lang/String;)Ljava/lang/String;
    transformIL.append(new INVOKEVIRTUAL(replacerMethodRef));
    // ASTORE 0
    transformIL.append(new ASTORE(0));
    // ILOAD 2
    transformIL.append(new ILOAD(COUNT));
    // ICONST_1
    transformIL.append(new ICONST(1));
    // IADD
    transformIL.append(new IADD());
    // I2B
    transformIL.append(new I2B());
    // I2B
    transformIL.append(new ISTORE(COUNT));


/*
    transformIL.append(new GETSTATIC(outFieldRef));
    transformIL.append(new ALOAD(0));
    transformIL.append(new INVOKEVIRTUAL(printlnMethodRef));
*/

    BranchHandle gotoJump = transformIL.append(new GOTO(ifHandle)); // GOTO L2
    //L3
    InstructionHandle endHandle = transformIL.append(new ALOAD(0)); // ALOAD 0
    transformIL.append(new ARETURN()); // ARETURN

    ifCheck.setTarget(endHandle);

    MethodGen trannsformMethodGen = new MethodGen(
      Const.ACC_PUBLIC | Const.ACC_STATIC,
      Type.STRING,
      new Type[]{Type.STRING},
      new String[]{"sql"},
      "transform",
      "com.yahoo.sdvornik.Transform",
      transformIL,
      cp
    );
    trannsformMethodGen.setMaxLocals();
    trannsformMethodGen.setMaxStack();

    genClass.addField(replacerFieldGen.getField());
    genClass.addField(patternFieldGen.getField());
    genClass.addMethod(clinitMethodGen.getMethod());
    genClass.addMethod(initMethodGen.getMethod());
    genClass.addMethod(trannsformMethodGen.getMethod());
    return genClass.getJavaClass();

  }

  public void createGetDatabaseMajorVersion(ClassGen modClass, Method oldMethod) {

    InstructionList instructionList = new InstructionList();

    instructionList.append(new LDC(modClass.getConstantPool().addInteger(conf.databaseMajorVersion)));
    instructionList.append(new IRETURN());

    MethodGen mg = new MethodGen(
      Const.ACC_PUBLIC,
      Type.INT,
      oldMethod.getArgumentTypes(),
      null,
      oldMethod.getName(),
      modClass.getClassName(),
      instructionList,
      modClass.getConstantPool()
    );
    mg.setMaxLocals();
    mg.setMaxStack();

    modClass.removeMethod(oldMethod);
    modClass.addMethod(mg.getMethod());
  }

  public void createGetDatabaseProductVersion(ClassGen modClass, Method oldMethod) {

    InstructionList instructionList = new InstructionList();

    instructionList.append(new LDC(modClass.getConstantPool().addString(conf.databaseProductVersion)));
    instructionList.append(new ARETURN());

    MethodGen mg = new MethodGen(
      Const.ACC_PUBLIC,
      Type.STRING,
      oldMethod.getArgumentTypes(),
      null,
      oldMethod.getName(),
      modClass.getClassName(),
      instructionList,
      modClass.getConstantPool()
    );
    mg.setMaxLocals();
    mg.setMaxStack();

    modClass.removeMethod(oldMethod);
    modClass.addMethod(mg.getMethod());
  }


  public Method createGetDatabaseProductName(JavaClass mod, Method method, ConstantPoolGen cp) {
    MethodGen mg = new MethodGen(method, mod.getClassName(), cp);

    InstructionList ils = new InstructionList();

    ils.append(new LDC(cp.addString(conf.databaseProductName)));
    ils.append(new ARETURN());

    mg.setMaxLocals();
    mg.setMaxStack();

    return mg.getMethod();
  }

  public void createGetDatabaseProductName(ClassGen modClass, Method oldMethod) {

    InstructionList instructionList = new InstructionList();

    instructionList.append(new LDC(modClass.getConstantPool().addString(conf.databaseProductName)));
    instructionList.append(new ARETURN());

    MethodGen mg = new MethodGen(
      Const.ACC_PUBLIC,
      Type.STRING,
      oldMethod.getArgumentTypes(),
      null,
      oldMethod.getName(),
      modClass.getClassName(),
      instructionList,
      modClass.getConstantPool()
    );
    mg.setMaxLocals();
    mg.setMaxStack();

    modClass.removeMethod(oldMethod);
    modClass.addMethod(mg.getMethod());
  }

}
