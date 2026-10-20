/*
 * Nombre: tokens.js
 * Descripción: Tokens de diseño de InspectIA (RP-08): los colores de los temas claro y oscuro, con tonos de
 *              cacao, y la letra y las medidas del prototipo. Son la única fuente de colores de la app.
 * Fecha de creación: 20/10/2026
 * Actualización: 20/10/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */

/**
 * Colores de cada tema, tomados de la tabla del README del prototipo (02_Analisis_Diseno/
 * Prototipo_Frontend). Cumplen WCAG AA: 4,5:1 para el texto y 3:1 para los bordes de los campos
 * (RNF-06 C4); tokens.test.js lo revisa.
 *
 * En los avisos, «principal» es el color del ícono y del borde, «texto» el de las letras y «fondo» el
 * del recuadro.
 */
export const colores = {
  claro: {
    fondo: '#FAF6F1',
    superficie: '#FFFFFF',
    barra: { fondo: '#6B3F22', texto: '#FFFFFF', marca: '#FFFFFF', subtitulo: '#F1E4D8', borde: '#6B3F22' },
    texto: '#2B1D14',
    textoSecundario: '#65503F',
    primario: '#6B3F22',
    sobrePrimario: '#FFFFFF',
    enlaceHover: '#4E2C17',
    seleccion: { fondo: '#F1E4D8', texto: '#6B3F22' },
    avatar: { fondo: '#F1E4D8', texto: '#6B3F22' },
    bordeCampo: '#8E7564',
    bordeTarjeta: '#E8DED3',
    lineaFila: '#F1E9E0',
    exito: { principal: '#2E6B34', texto: '#2E6B34', fondo: '#E7F1E7' },
    advertencia: { principal: '#8A5300', texto: '#6B4100', fondo: '#FBF0DC' },
    error: { principal: '#B3261E', texto: '#8C1D18', fondo: '#FBEAE8' },
    informacion: { principal: '#265C8A', texto: '#1B4468', fondo: '#E6EEF6' },
  },
  oscuro: {
    fondo: '#17110D',
    superficie: '#221914',
    barra: { fondo: '#2A1F18', texto: '#F4EDE6', marca: '#E3AD78', subtitulo: '#CDBBAA', borde: '#3A2C23' },
    texto: '#F4EDE6',
    textoSecundario: '#CDBBAA',
    primario: '#E3AD78',
    sobrePrimario: '#2B1D14',
    enlaceHover: '#F4C99A',
    seleccion: { fondo: '#3A2A1F', texto: '#E3AD78' },
    avatar: { fondo: '#E3AD78', texto: '#2B1D14' },
    bordeCampo: '#9C8573',
    bordeTarjeta: '#3A2C23',
    lineaFila: '#3A2C23',
    exito: { principal: '#8DCB85', texto: '#8DCB85', fondo: '#1E2A1C' },
    advertencia: { principal: '#F0C063', texto: '#F0C063', fondo: '#2E2414' },
    error: { principal: '#FFB4A9', texto: '#FFB4A9', fondo: '#3A1F1C' },
    informacion: { principal: '#9CC8F2', texto: '#9CC8F2', fondo: '#1A2633' },
  },
};

/**
 * Marcas sobre las fotos de la muestra (RF-11): no cambian con el tema, porque la foto tampoco cambia.
 * Cada una va con su forma (chulo, equis o signo de pregunta), así el color no es la única señal.
 */
export const marcasDeFoto = { sano: '#2E6B34', noSano: '#B3261E', dudoso: '#8A5300' };

/** Letra: Roboto 400, 500 y 700, servida desde la app. Tamaños en píxeles. */
export const letra = {
  familia: 'Roboto, system-ui, sans-serif',
  pesos: { regular: 400, media: 500, negrita: 700 },
  titulo: { pc: 30, celular: 24 },
  subtitulo: 20,
  texto: 16,
  ayuda: 14,
  chip: 13,
};

/** Medidas en píxeles: barra, menú, contenido, controles y zonas táctiles. */
export const medidas = {
  barra: 64,
  menu: 240,
  contenido: { arriba: 32, lados: 40, ladosCelular: 16, anchoMaximo: 1120 },
  tarjeta: { radio: 12, relleno: 24 },
  boton: { alto: 48, altoEnTabla: 40, radio: 8 },
  campo: { alto: 48, radio: 8 },
  chip: { radio: 16 },
  zonaTactil: 44,
  foco: { grosor: 2, separacion: 2 },
};
