import firebase_admin
from firebase_admin import credentials, firestore
import time
import random

# Configuración de Firebase:
cred = credentials.Certificate("serviceAccountKey.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

def simular_sesion():
    
    COLECCION_SESIONES = "sesiones"
    SUBCOLECCION_ALUMNADO = "alumnado"
    
    CAMPO_PIN = "pin"               
    CAMPO_NICK = "nick"             
    CAMPO_LOCATION = "ubicacion"
    CAMPO_LOC = "ubic"
    CAMPO_TIMEST = "timest"
    # =====================================================================

    try:
        pin_input = int(input("Introduce el PIN numérico de la sesión activa: "))
    except ValueError:
        print("El PIN debe ser un número entero.")
        return
    
    # Buscar la sesión por PIN:
    sesiones_ref = db.collection(COLECCION_SESIONES)
    query = sesiones_ref.where(CAMPO_PIN, "==", pin_input).limit(1).stream()
    
    sesion_doc = None
    for doc in query:
        sesion_doc = doc
        break
        
    if not sesion_doc:
        print("No se encontró ninguna sesión con ese PIN.")
        return

    sesion_id = sesion_doc.id
    print(f"Sesión encontrada: {sesion_id}")
    
    # Coordenadas iniciales (ajustar a la zona de la ruta):
    lat_base = 38.1174
    lon_base = -3.08373


    # Alumnado ficticio:
    alumnos_nombres = ["Alpha", "Beta", "Gamma", "Delta"]
    alumnos_refs = []

    print("Creando alumnado e inyectando ubicaciones iniciales...")

    for nombre in alumnos_nombres:
        ref = sesiones_ref.document(sesion_id).collection(SUBCOLECCION_ALUMNADO).document()
        ref.set({
            CAMPO_NICK: nombre,
            CAMPO_LOCATION: {
                CAMPO_LOC: firestore.GeoPoint(lat_base, lon_base),
                CAMPO_TIMEST: firestore.SERVER_TIMESTAMP
            }
        })
        alumnos_refs.append({"ref": ref, "lat": lat_base, "lon": lon_base})

    # Simulación del movimiento:
    print("¡Los alumnos están en movimiento! Revisa el mapa de la app. Pulsa Ctrl+C para detener.")
    try:
        while True:
            for alumno in alumnos_refs:
                alumno["lat"] += random.uniform(-0.0001, 0.0001)
                alumno["lon"] += random.uniform(-0.0001, 0.0001)
                alumno["ref"].update({
                    f"{CAMPO_LOCATION}.{CAMPO_LOC}": firestore.GeoPoint(alumno["lat"], alumno["lon"]),
                    f"{CAMPO_LOCATION}.{CAMPO_TIMEST}": firestore.SERVER_TIMESTAMP
                })
            
            time.sleep(10) # Actualiza cada 10 segundos
    except KeyboardInterrupt:
        print("\nSimulación detenida correctamente.")

if __name__ == "__main__":
    simular_sesion()
