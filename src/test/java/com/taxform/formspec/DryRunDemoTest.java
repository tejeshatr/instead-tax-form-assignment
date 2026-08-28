package com.taxform.formspec;

import com.taxform.formspec.demo.DryRunDemo;
import com.taxform.formspec.fill.FillPlan;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the demo entry point runs end-to-end and renders the table shown
 * in the walkthrough video.
 */
class DryRunDemoTest {

    @Test
    void demoRunsAndRendersFillPlan() {
        FillPlan plan = DryRunDemo.run();
        String rendered = plan.render();
        assertTrue(rendered.contains("Total: 62 entries"), rendered);
        assertTrue(rendered.contains("l1a.wagesAmt"), rendered);
        assertTrue(rendered.contains("$123,400"), rendered);
        assertTrue(rendered.contains("SKIPPED_MISSING"), rendered);
        assertTrue(rendered.contains("Annotations:"), rendered);
        assertTrue(rendered.contains("Add your name here"), rendered);
        assertTrue(rendered.contains("[left, middle]"), rendered);
        assertTrue(rendered.contains("DRAFT — DO NOT FILE"), rendered);
        assertTrue(!rendered.contains("ERROR_"), rendered);
    }
}
