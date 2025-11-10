# PIC k150 Programming

**PIC k150 Programming** es una aplicación Android que permite programar microcontroladores PIC utilizando el programador PIC k150, todo desde la comodidad de un teléfono inteligente Android. La aplicación se basa en el protocolo **P018** de **KITSRUS Programmer Firmware Protocol P018**, versión de agosto de 2004, lo que la hace compatible con todos los dispositivos que implementan este protocolo. Esta innovadora solución permite a los entusiastas de la electrónica y desarrolladores programar sus microcontroladores sin necesidad de un equipo de escritorio, usando únicamente su dispositivo Android.

## Características

- **Programación sin PC**: Permite programar microcontroladores PIC directamente desde tu teléfono Android usando el programador PIC k150 o cualquier dispositivo compatible con el protocolo P018 de KITSRUS.
- **Protocolo P018**: Basado en el protocolo KITSRUS Programmer Firmware Protocol P018, lo que garantiza una amplia compatibilidad con dispositivos que utilicen este estándar.
- **Librerías USB-Serial**: Utiliza la librería `usb-serial-for-android` para la comunicación USB entre el teléfono Android y el programador PIC k150.
- **Fácil uso**: La interfaz de usuario está diseñada para ser intuitiva y fácil de usar, permitiendo a los usuarios cargar fácilmente archivos de programación y realizar el proceso con mínimos pasos.
- **Compatibilidad**: La aplicación funciona con todos los dispositivos que implementan el protocolo P018 de KITSRUS, lo que incluye una variedad de programadores y dispositivos PIC.

## Dispositivos compatibles

La aplicación es compatible con dispositivos que sigan el **Protocolo P018** de **KITSRUS**, entre ellos:

- **Programador PIC k150**
- **Dispositivos compatibles con el protocolo P018 de KITSRUS**

Este protocolo es ampliamente utilizado para la programación de microcontroladores PIC, lo que asegura una gran flexibilidad y opciones a la hora de elegir el hardware adecuado.

## Dependencias y Librerías

El proyecto está basado en las siguientes librerías y recursos:

- **usb-serial-for-android**: Esta librería se utiliza para establecer la comunicación serial entre el dispositivo Android y el programador PIC k150 a través de USB. Puedes encontrar el repositorio oficial [aquí](https://github.com/mik3y/usb-serial-for-android).
  
- **picprogrammer**: El proyecto original que implementa la lógica de programación basada en el protocolo P018. El código fuente puede ser consultado [aquí](https://github.com/almost/picprogrammer.git).

- **picpro**: Este proyecto implementa varias versiones del protocolo de KITSRUS. El código fuente puede ser consultado [aquí](https://github.com/Salamek/picpro).

- **AndroidIDE**: El proyecto fue desarrollado íntegramente en **AndroidIDE**, una potente plataforma para desarrollar aplicaciones Android directamente desde un dispositivo Android. 
  - Repositorio oficial: [https://github.com/AndroidIDEOfficial/AndroidIDE.git](https://github.com/AndroidIDEOfficial/AndroidIDE.git)
  - Página oficial: [https://m.androidide.com/](https://m.androidide.com/)
 
- **Android Code Studio**: El proyecto ahora se desarrolla en **Android Code Studio**, una potente plataforma para desarrollar aplicaciones Android directamente desde un dispositivo Android. 
  - Repositorio oficial: [https://github.com/AndroidCSOfficial/android-code-studio.git](https://github.com/AndroidCSOfficial/android-code-studio.git)

## Enlaces útiles

- **KITSRUS - Página oficial**: [https://www.kitsrus.com](https://www.kitsrus.com)
  
- **Link de Google Play**: [aquí.](https://play.google.com/store/apps/details?id=com.diamon.pic)
  
## Instrucciones de instalación

1. Clona el repositorio:
   ```bash
   git clone https://github.com/Danielk10/PIC-k150-Programing.git


2. Abre el proyecto en Android Studio o AndroidIDE.

3. Conecta tu dispositivo Android a tu PC y habilita la depuración USB.

4. Instala la aplicación en tu dispositivo Android.

5. Conecta el programador PIC k150 o cualquier dispositivo compatible con el protocolo P018 a tu teléfono Android a través de un adaptador USB OTG.

6. Abre la aplicación y comienza a programar tu microcontrolador PIC.

## Autores y Agradecimientos

- **Desarrollador Principal**: Danielk10
  
- **Contribuyentes**: Agradecimientos a los colaboradores de las librerías **usb-serial-for-android** y **picprogrammer**, y al equipo detrás de **AndroidIDE** por proporcionar las herramientas necesarias para el desarrollo del proyecto.

- **Protocolo P018**: Gracias a **KITSRUS** por el desarrollo y documentación del protocolo P018 que permite la interoperabilidad de los dispositivos de programación PIC.

## Contribuciones

Las contribuciones son bienvenidas. Si deseas mejorar la aplicación o agregar nuevas características, siéntete libre de hacer un fork del proyecto y enviar un pull request. Asegúrate de seguir las mejores prácticas de codificación y de realizar pruebas exhaustivas antes de enviar cualquier contribución.

## Licencia

Este proyecto está licenciado bajo la **Licencia GPL-2.0**. Para más detalles, consulta el archivo `LICENSE`.
