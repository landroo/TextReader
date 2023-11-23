/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: ISmartDictInterfaceFraBig.aidl
 */
package org.landroo.frahunbig;
// C:\Develop\Android\android-sdk\platform-tools\aidl.exe

public interface ISmartDictInterfaceFraBig extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.landroo.frahunbig.ISmartDictInterfaceFraBig
{
private static final java.lang.String DESCRIPTOR = "org.landroo.frahunbig.ISmartDictInterfaceFraBig";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.landroo.frahunbig.ISmartDictInterfaceFraBig interface,
 * generating a proxy if needed.
 */
public static org.landroo.frahunbig.ISmartDictInterfaceFraBig asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.landroo.frahunbig.ISmartDictInterfaceFraBig))) {
return ((org.landroo.frahunbig.ISmartDictInterfaceFraBig)iin);
}
return new org.landroo.frahunbig.ISmartDictInterfaceFraBig.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_fillWordList:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
int _arg1;
_arg1 = data.readInt();
int _arg2;
_arg2 = data.readInt();
java.lang.String[] _result = this.fillWordList(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeStringArray(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.landroo.frahunbig.ISmartDictInterfaceFraBig
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public java.lang.String[] fillWordList(java.lang.String sWord, int iLang, int iMode) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sWord);
_data.writeInt(iLang);
_data.writeInt(iMode);
mRemote.transact(Stub.TRANSACTION_fillWordList, _data, _reply, 0);
_reply.readException();
_result = _reply.createStringArray();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_fillWordList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public java.lang.String[] fillWordList(java.lang.String sWord, int iLang, int iMode) throws android.os.RemoteException;
}
