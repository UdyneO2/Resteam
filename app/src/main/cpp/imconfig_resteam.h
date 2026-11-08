#pragma once
// ImGui context per-thread: UI utama dan overlay jalan di thread berbeda.
struct ImGuiContext;
extern thread_local ImGuiContext* MyImGuiTLS;
#define GImGui MyImGuiTLS
