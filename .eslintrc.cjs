module.exports ={
  "env": {"node": true},
  "parserOptions": {
    "sourceType": "module",
    "ecmaVersion": 2020,
  },
  "extends": ["eslint:recommended", "plugin:json/recommended"],
  "globals": {"Promise": true, "ArrayBuffer": true, "DataView": true},
  "rules": {
    "indent": ["error", 2, {"SwitchCase": 1}],
    "linebreak-style": ["error", "unix"],
    "quotes": ["error", "double", {"avoidEscape": true}],
    "semi": ["error", "never"],
    "no-console": 0,
    "no-unused-vars": ["warn", { "argsIgnorePattern": "^_" }],
  }
}
