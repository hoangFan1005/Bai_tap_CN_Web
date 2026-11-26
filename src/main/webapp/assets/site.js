"use strict";

const accountMenu = document.querySelector(".account-menu");

if (accountMenu) {
    document.addEventListener("pointerdown", (event) => {
        if (accountMenu.open && !accountMenu.contains(event.target)) {
            accountMenu.open = false;
        }
    });

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape" && accountMenu.open) {
            accountMenu.open = false;
            accountMenu.querySelector("summary").focus();
        }
    });
}

const productImages = ".product-card__image img, .product-detail__image img, .table-product img";
document.querySelectorAll(`img[data-fallback-image], .auth-side img, ${productImages}`).forEach((image) => {
    image.addEventListener("error", () => { image.hidden = true; }, { once: true });
    if (image.complete && image.naturalWidth === 0) {
        image.hidden = true;
    }
});

document.querySelectorAll("form[data-confirm]").forEach((form) => {
    form.addEventListener("submit", (event) => {
        if (!window.confirm(form.dataset.confirm)) {
            event.preventDefault();
        }
    });
});
