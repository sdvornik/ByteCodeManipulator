package com.yahoo.sdvornik;

/**
 * @author Serg Dvornik <sdvornik@yahoo.com>
 */
import org.apache.bcel.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.classfile.*;

public class MakingConstructor implements Constants {
  protected JavaClass originCode;
  protected ClassGen modifiedCode;

  public MakingConstructor( String originCode ) throws ClassNotFoundException {
    this.originCode = Repository.lookupClass( originCode );
    this.modifiedCode = new ClassGen( this.originCode );
  }

  public static void main( String args[] ) {
    if( args.length != 1 ) {
      System.out.println( "Вызов: java MakingConstructor <имя_класса_без_расширения>" );
      System.exit( -1 );
    }

    try {
      MakingConstructor modifiedByteCode = new MakingConstructor( args[0] );
      if( modifiedByteCode.originCode == null ) {
        // заданный class-файл невозможно открыть для считывания содержимого
        throw new Exception();
      } else {
        // добавление пустого конструктора по умолчанию в анализируемый класс
        modifiedByteCode.modifiedCode.addEmptyConstructor( ACC_PUBLIC );
        // сохранение измененного класса в отдельный class-файл
        modifiedByteCode.modifiedCode.getJavaClass().dump( "new_prim.class" );
      }
    } catch( Exception e ) {
      System.err.println( "Необходимо задать имя class-файла без расширения" );
    }
  }
}
