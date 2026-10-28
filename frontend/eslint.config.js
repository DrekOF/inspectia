/*
 * Nombre: eslint.config.js
 * Descripción: Reglas de ESLint del frontend: JavaScript moderno, las reglas de los hooks de React, JSDoc en
 *              cada función y componente exportados (RP-09) y los colores solo en los tokens del tema (RP-08).
 *              No revisa lo compilado, los reportes ni la copia de la demostración de otra temática.
 * Fecha de creación: 20/10/2026
 * Actualización: 28/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.1
 */
import js from '@eslint/js';
import jsdoc from 'eslint-plugin-jsdoc';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';

/** Un color escrito a mano, como «#6B3F22»: fuera de src/tema/tokens.js no se permite (RP-08). */
const COLOR_A_MANO = {
  selector: 'Literal[value=/^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/]',
  message: 'Los colores salen de src/tema/tokens.js por medio del tema, no se escriben a mano (RP-08).',
};

export default [
  { ignores: ['dist/', 'coverage/', 'reports/', 'test-results/', 'playwright-report/', 'blob-report/', '.stryker-tmp/',
    '.demostracion/'] },
  js.configs.recommended,
  jsdoc.configs['flat/recommended-error'],
  {
    files: ['**/*.{js,jsx}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: { ecmaFeatures: { jsx: true } },
      globals: { ...globals.browser, ...globals.node },
    },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      ...reactHooks.configs['recommended-latest'].rules,
      'jsdoc/require-jsdoc': ['error', {
        publicOnly: true,
        require: { FunctionDeclaration: true, ArrowFunctionExpression: true, FunctionExpression: true },
      }],
      'jsdoc/tag-lines': ['error', 'any', { startLines: 1 }],
      'no-restricted-syntax': ['error', COLOR_A_MANO],
    },
  },
  {
    files: ['src/tema/tokens.js', '**/*.test.{js,jsx}', 'e2e/**/*.js', 'selenium/**/*.js'],
    rules: { 'no-restricted-syntax': 'off' },
  },
];
