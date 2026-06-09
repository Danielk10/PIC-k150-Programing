# 🤖 Guía de Flujo de Trabajo para Agentes de IA

Esta guía establece el estándar operativo obligatorio para cualquier agente de IA que trabaje en el proyecto **PIC k150 Programming**. El objetivo es mantener la integridad técnica, la consistencia de la documentación y la sincronización continua del repositorio.

## 1. Fase de Investigación (Contexto)
- **Siempre** leer `GEMINI.md` al inicio de la sesión.
- Verificar el estado actual del proyecto (versiones en `build.gradle`, estado de git, y configuración del SDK).

## 2. Actualización de Código y Configuración
- Al modificar versiones (`versionCode`, `versionName`), dependencias o configuraciones del SDK/NDK, se deben aplicar los cambios de forma quirúrgica.
- **Seguridad**: Nunca subir archivos sensibles como `google-services.json` o llaves privadas. Verificar `.gitignore`.

## 3. Consistencia de la Documentación (Crucial)
Cada cambio en el código o configuración **DEBE** verse reflejado inmediatamente en los siguientes archivos:
1.  **`GEMINI.md`**: El índice de conocimiento del agente.
2.  **`README.md`**: La cara pública del proyecto (actualizar badges y secciones técnicas).
3.  **`resumen_instalacion.md`**: El log histórico de cambios en el entorno.
4.  **`guia_uso_sdk.md`**: Guía técnica de comandos (actualizar versiones de API/Build-Tools).

## 4. Verificación Obligatoria
- **Nunca** realizar un commit sin haber validado el cambio.
- Comando estándar de validación: `./gradlew assembleDebug`.
- Si existen problemas de espacio en disco en el entorno, utilizar `/tmp/android-sdk` para el SDK y configurar `local.properties` en consecuencia.

## 5. Protocolo de Git
- **Autoría**: Configurar siempre el autor como:
  - Nombre: `Danielk10`
  - Email: `danielpdiamon@gmail.com`
- **Mensajes de Commit**: Claros, concisos y con prefijos (e.g., `feat:`, `fix:`, `docs:`, `chore:`).
- **Sincronización**: Realizar `git push origin main` inmediatamente después de cada commit verificado. No dejar cambios pendientes localmente.

## 6. Publicación de Prelanzamiento (GitHub)
- Antes de realizar un prelanzamiento, **preguntar siempre al usuario** si desea subir la APK a GitHub.
- Si el usuario acepta, usar el comando de `gh release create` incrementando la versión (vX.Y.Z-beta) según corresponda:
  - **Comando Android Nativo**:
    ```bash
    gh release create v0.1.0-beta app/build/outputs/apk/debug/app-debug.apk --title "Versión Alfa 0.1.0" --notes "Primera compilación de prueba del juego." --prerelease
    ```

---
*Esta guía es un mandato para garantizar que el proyecto se mantenga profesional, documentado y funcional en cada iteración.*
