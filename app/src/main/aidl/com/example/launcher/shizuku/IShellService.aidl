package com.example.launcher.shizuku;

interface IShellService {
    // Transaction ID 16777114 wajib untuk destroy() (konvensi Shizuku UserService)
    void destroy() = 16777114;

    // Mengembalikan "<exitCode>\n<output>"
    String exec(in String[] command) = 1;
}
