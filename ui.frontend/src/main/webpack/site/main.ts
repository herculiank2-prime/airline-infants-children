// Stylesheets
import "./main.scss";

// Component scripts. main.ts must not glob its own folder: the self-import makes webpack recurse.
import "../components/**/*.ts";
