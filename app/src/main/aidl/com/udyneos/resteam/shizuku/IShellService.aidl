package com.udyneos.resteam.shizuku;

interface IShellService {
    // Transaction ID 16777114 wajib untuk destroy() (konvensi Shizuku UserService)
    void destroy() = 16777114;

    // Mengembalikan "<exitCode>\n<output>"
    String exec(in String[] command) = 1;

    // Sama seperti exec, tetapi proses dimatikan paksa jika melewati timeoutMs (exit 124)
    String execTimeout(in String[] command, long timeoutMs) = 2;
}
