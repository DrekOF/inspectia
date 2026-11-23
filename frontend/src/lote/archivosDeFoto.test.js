/*
 * Nombre: archivosDeFoto.test.js
 * Descripción: Pruebas de la revisión de una foto antes de subirla (RF-05 C3): el formato sale de sus primeros bytes,
 *              no del nombre ni del tipo que dice el navegador, y el tamaño tiene su límite de 10 MB.
 * Fecha de creación: 23/11/2026
 * Actualización: 23/11/2026
 * Autor: Equipo InspectIA
 * Versión: 1.0
 */
import { describe, expect, it } from 'vitest';
import { archivo } from '../pruebas/fotos.js';
import { MAXIMO_DE_BYTES, revisarArchivo } from './archivosDeFoto.js';

describe('Revisar una foto antes de subirla', () => {
  it('RF-05 · C3 · un JPG y un PNG pasan con el tipo de sus primeros bytes, aunque el navegador diga otro o ninguno', async () => {
    expect(await revisarArchivo(archivo('granos.jpg', 'image/jpeg'))).toEqual({ tipo: 'image/jpeg' });
    expect(await revisarArchivo(archivo('granos.png', 'image/png'))).toEqual({ tipo: 'image/png' });
    expect(await revisarArchivo(archivo('IMG_0001', '', 64, 'image/jpeg'))).toEqual({ tipo: 'image/jpeg' });
    expect(await revisarArchivo(archivo('captura.jpg', 'image/jpeg', 64, 'image/png'))).toEqual({ tipo: 'image/png' });
  });

  it('RF-05 · C3 · un PDF, un archivo vacío, uno que se llama .jpg y no lo es, y un PNG cortado se rechazan con el mensaje del formato', async () => {
    const malos = [
      archivo('informe.pdf', 'application/pdf'),
      archivo('vacia.jpg', 'image/jpeg', 0),
      archivo('falsa.jpg', 'image/jpeg', 64, 'application/pdf'),
      archivo('cortada.png', 'image/png', 4),
    ];
    for (const malo of malos) {
      expect(await revisarArchivo(malo), malo.name).toEqual({ error: 'foto.formato.no_permitido' });
    }
  });

  it('RF-05 · C3 · una foto de 10.485.760 bytes pasa y una de 10.485.761 se rechaza con el mensaje del tamaño (prueba de límite)', async () => {
    expect(MAXIMO_DE_BYTES).toBe(10_485_760);
    expect(await revisarArchivo(archivo('justa.jpg', 'image/jpeg', 10_485_760))).toEqual({ tipo: 'image/jpeg' });
    expect(await revisarArchivo(archivo('grande.png', 'image/png', 10_485_761)))
      .toEqual({ error: 'foto.tamano_bytes.demasiado_grande' });
  });

  it('RF-05 · C3 · un archivo que no es JPG ni PNG y además pasa de 10 MB recibe el mensaje del formato, que pide elegir otra foto', async () => {
    expect(await revisarArchivo(archivo('video.mp4', 'video/mp4', 10_485_761, 'application/pdf')))
      .toEqual({ error: 'foto.formato.no_permitido' });
  });
});
