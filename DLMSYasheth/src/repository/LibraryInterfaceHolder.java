package repository;

/**
* repository/LibraryInterfaceHolder.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from LibraryInterface.idl
* Friday, 22 March, 2019 10:53:02 PM EDT
*/

public final class LibraryInterfaceHolder implements org.omg.CORBA.portable.Streamable
{
  public repository.LibraryInterface value = null;

  public LibraryInterfaceHolder ()
  {
  }

  public LibraryInterfaceHolder (repository.LibraryInterface initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = repository.LibraryInterfaceHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    repository.LibraryInterfaceHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return repository.LibraryInterfaceHelper.type ();
  }

}
