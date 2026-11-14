# NotesAi
## Licencias de fuentes

Las fuentes incluidas en la app (Alfa Slab One, Pacifico, Lobster y Courgette) usan la licencia SIL Open Font License; los textos están en `licenses/fonts/`.

## Sincronización en la nube

Con la sesión de Google iniciada, las notas se sincronizan con Cloud Firestore en `users/{uid}/notes/{noteId}` y siguen funcionando sin conexión (persistencia offline de Firestore).

1. En Firebase Console crea la base de datos de Firestore (modo producción).
2. En **Firestore Database > Reglas** pega el contenido de `firestore.rules` y publícalo.

Las imágenes, dibujos y audios no se suben (Firestore no guarda archivos): esas notas sincronizan su texto, formato y etiqueta, y el archivo queda en el dispositivo donde se creó.
