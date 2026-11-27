import { pushEvent, readComponentData } from "../site/datalayer";

/**
 * Accordion behaviour for airline/components/travelquestions (AC-01, DL-01, ADR-018). The collapsed state
 * is rendered on the server; native buttons give Tab, Enter and Space for free, so only click is handled.
 * Collapsing restores hidden="until-found" rather than plain hidden, so find-in-page keeps working.
 */
type Trigger = "click" | "find-in-page";

const ROOT = ".cmp-travelquestions";
const BUTTON = ".cmp-travelquestions__button";

function panelFor(button: HTMLButtonElement): HTMLElement | null {
    const id = button.getAttribute("aria-controls");
    return id ? document.getElementById(id) : null;
}

function report(button: HTMLButtonElement, expanded: boolean, trigger: Trigger): void {
    const item = button.closest(".cmp-travelquestions__item");
    const accordion = button.closest(ROOT);
    const data = readComponentData(item);
    const componentId = data?.id || button.dataset.itemId || button.id;
    pushEvent(expanded ? "airline:accordion:expand" : "airline:accordion:collapse", {
        path: `component.${componentId}`,
        componentId,
        title: data?.title || button.textContent?.trim(),
        parentId: data?.parentId || accordion?.id,
        trigger,
    });
}

function setExpanded(button: HTMLButtonElement, expanded: boolean, trigger: Trigger): void {
    const panel = panelFor(button);
    if (!panel) {
        return;
    }
    button.setAttribute("aria-expanded", String(expanded));
    if (expanded) {
        panel.removeAttribute("hidden");
    } else {
        panel.setAttribute("hidden", "until-found");
    }
    report(button, expanded, trigger);
}

document.addEventListener("click", (event: MouseEvent) => {
    const target = event.target as Element | null;
    const button = target && typeof target.closest === "function"
        ? target.closest<HTMLButtonElement>(`${ROOT} ${BUTTON}`)
        : null;
    if (button) {
        setExpanded(button, button.getAttribute("aria-expanded") !== "true", "click");
    }
});

// beforematch does not bubble, so it is captured at the document. The browser removes hidden itself.
document.addEventListener("beforematch", (event: Event) => {
    const panel = event.target as HTMLElement | null;
    const button = panel?.id ? document.getElementById(panel.id.replace(/-panel$/, "-button")) : null;
    if (button instanceof HTMLButtonElement && button.getAttribute("aria-expanded") !== "true") {
        button.setAttribute("aria-expanded", "true");
        report(button, true, "find-in-page");
    }
}, true);
