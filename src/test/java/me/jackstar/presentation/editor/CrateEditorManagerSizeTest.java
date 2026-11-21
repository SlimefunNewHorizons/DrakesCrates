package me.jackstar.drakescrates.presentation.editor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CrateEditorManagerSizeTest {

    @Test
    void clampsChestSizeBetween9And54() {
        assertEquals(9, CrateEditorManager.computeChestSize(0));
        assertEquals(9, CrateEditorManager.computeChestSize(1));
        assertEquals(9, CrateEditorManager.computeChestSize(9));
        assertEquals(18, CrateEditorManager.computeChestSize(10));
        assertEquals(54, CrateEditorManager.computeChestSize(54));
        assertEquals(54, CrateEditorManager.computeChestSize(60));
        assertEquals(54, CrateEditorManager.computeChestSize(120));
    }
}
