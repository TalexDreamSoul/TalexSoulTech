document.documentElement.classList.add("has-js");

const toggle = document.getElementById("nav-toggle");
const navigation = document.getElementById("primary-nav");

if (toggle && navigation) {
  const label = toggle.querySelector(".sr-only");

  const setOpen = (open, { returnFocus = false } = {}) => {
    toggle.setAttribute("aria-expanded", String(open));
    navigation.dataset.open = String(open);
    if (label) label.textContent = open ? "关闭导航" : "打开导航";
    document.body.classList.toggle("nav-open", open);
    if (!open && returnFocus) toggle.focus();
  };

  const isOpen = () => toggle.getAttribute("aria-expanded") === "true";

  toggle.addEventListener("click", () => setOpen(!isOpen()));

  navigation.addEventListener("click", (event) => {
    if (event.target.closest("a")) setOpen(false);
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && isOpen()) setOpen(false, { returnFocus: true });
  });

  document.addEventListener("click", (event) => {
    if (!isOpen() || navigation.contains(event.target) || toggle.contains(event.target)) return;
    setOpen(false);
  });

  const desktopQuery = window.matchMedia("(min-width: 64.001rem)");
  const handleDesktop = (event) => {
    if (event.matches) setOpen(false);
  };
  desktopQuery.addEventListener?.("change", handleDesktop);
}
