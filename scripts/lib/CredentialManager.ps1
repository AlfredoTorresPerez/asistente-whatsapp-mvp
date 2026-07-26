$script:typeAdded = $false

function Initialize-CredentialManager {
  if (-not $script:typeAdded) {
    Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
using System.Text;

public static class WinCredManager
{
    public enum CredType { GENERIC = 1 }
    public enum PersistType { SESSION = 1, LOCAL_MACHINE = 2, ENTERPRISE = 3 }

    [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool CredWrite(ref CREDENTIAL userCredential, uint flags);

    [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool CredRead(string target, CredType type, uint reservedFlag, out IntPtr credentialPtr);

    [DllImport("advapi32.dll", SetLastError = true, CharSet = CharSet.Unicode)]
    private static extern bool CredDelete(string target, CredType type, uint flags);

    [DllImport("advapi32.dll", SetLastError = true)]
    private static extern void CredFree(IntPtr cred);

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct CREDENTIAL
    {
        public uint Flags;
        public uint Type;
        public IntPtr TargetName;
        public IntPtr Comment;
        public System.Runtime.InteropServices.ComTypes.FILETIME LastWritten;
        public uint CredentialBlobSize;
        public IntPtr CredentialBlob;
        public uint Persist;
        public uint AttributeCount;
        public IntPtr Attributes;
        public IntPtr TargetAlias;
        public IntPtr UserName;
    }

    public static void Write(string target, string username, string secret, PersistType persist = PersistType.LOCAL_MACHINE)
    {
        byte[] bytes = Encoding.Unicode.GetBytes(secret);
        IntPtr targetPtr = Marshal.StringToCoTaskMemUni(target);
        IntPtr userPtr = Marshal.StringToCoTaskMemUni(username);
        IntPtr blobPtr = Marshal.AllocCoTaskMem(bytes.Length);
        Marshal.Copy(bytes, 0, blobPtr, bytes.Length);

        CREDENTIAL cred = new CREDENTIAL
        {
            Type = (uint)CredType.GENERIC,
            TargetName = targetPtr,
            UserName = userPtr,
            CredentialBlob = blobPtr,
            CredentialBlobSize = (uint)bytes.Length,
            Persist = (uint)persist,
            Comment = IntPtr.Zero,
            Attributes = IntPtr.Zero,
            AttributeCount = 0,
            TargetAlias = IntPtr.Zero,
            Flags = 0
        };

        if (!CredWrite(ref cred, 0))
            throw new System.ComponentModel.Win32Exception(Marshal.GetLastWin32Error());

        Marshal.FreeCoTaskMem(targetPtr);
        Marshal.FreeCoTaskMem(userPtr);
        Marshal.FreeCoTaskMem(blobPtr);
    }

    public static string Read(string target)
    {
        IntPtr credPtr;
        if (!CredRead(target, CredType.GENERIC, 0, out credPtr))
            return null;

        try
        {
            CREDENTIAL cred = (CREDENTIAL)Marshal.PtrToStructure(credPtr, typeof(CREDENTIAL));
            byte[] bytes = new byte[cred.CredentialBlobSize];
            Marshal.Copy(cred.CredentialBlob, bytes, 0, bytes.Length);
            return Encoding.Unicode.GetString(bytes).TrimEnd('\0');
        }
        finally
        {
            CredFree(credPtr);
        }
    }

    public static void Delete(string target)
    {
        CredDelete(target, CredType.GENERIC, 0);
    }
}
"@
    $script:typeAdded = $true
  }
}

function Set-LocalSecret {
  param([string]$Name, [string]$Value)
  Initialize-CredentialManager
  $target = "asistente-local/$Name"
  [WinCredManager]::Write($target, $Name.ToLower(), $Value)
}

function Get-LocalSecret {
  param([string]$Name)
  Initialize-CredentialManager
  $target = "asistente-local/$Name"
  return [WinCredManager]::Read($target)
}

function Remove-LocalSecret {
  param([string]$Name)
  Initialize-CredentialManager
  $target = "asistente-local/$Name"
  try { [WinCredManager]::Delete($target) } catch {}
}

