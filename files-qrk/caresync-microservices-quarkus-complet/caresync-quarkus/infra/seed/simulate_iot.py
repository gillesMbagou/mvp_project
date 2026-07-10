#!/usr/bin/env python3
"""Simulateur IoT CareSync — publie en MQTT le JSON attendu par IoTProcessor.java
(patientId, deviceSerial, deviceType, loincCode, value, unit, ts), sur les topics
caresync/devices/<serial>/{glucose,spo2,weight}. Valeurs qui fluctuent autour de
la baseline de chaque patient, avec un pic occasionnel pour démontrer le flux
d'alerte (glucose/SpO2/poids -> IoTProcessor -> Kafka -> alert-svc -> dashboard).

Aucune dépendance sur un dispositif physique : c'est la source de données IoT
proposée pour cet environnement de démo/dev (pas de matériel disponible).
"""
import json
import random
import time
import argparse
from pathlib import Path

import paho.mqtt.client as mqtt

TOPIC_BY_TYPE = {
    "GLUCOMETER": "glucose",
    "PULSE_OXIMETER": "spo2",
    "SCALE": "weight",
}

def jitter(base, pct=0.06):
    return base * random.uniform(1 - pct, 1 + pct)

def next_value(device):
    devtype = device["deviceType"]
    spike = random.random() < 0.08  # ~1 mesure sur 12 sort de la plage normale
    if devtype == "GLUCOMETER":
        v = jitter(device["baseGlucose"])
        if spike:
            v = random.choice([random.uniform(0.3, 0.55), random.uniform(3.2, 4.5)])
        return round(v, 2)
    if devtype == "PULSE_OXIMETER":
        v = jitter(device["baseSpO2"], pct=0.02)
        if spike:
            v = random.uniform(78, 88)
        return round(min(v, 100.0), 1)
    if devtype == "SCALE":
        v = jitter(device["weightKg"], pct=0.01)
        if spike:
            v = random.uniform(85, 98)
        return round(v, 1)
    raise ValueError(devtype)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--port", type=int, default=1883)
    parser.add_argument("--interval", type=float, default=6.0, help="secondes entre 2 mesures par dispositif")
    parser.add_argument("--devices-file", default=str(Path(__file__).parent / "devices_for_simulator.json"))
    args = parser.parse_args()

    devices = json.loads(Path(args.devices_file).read_text())
    print(f"[sim] {len(devices)} dispositifs chargés depuis {args.devices_file}")

    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id="caresync-iot-simulator")
    client.connect(args.host, args.port, keepalive=30)
    client.loop_start()
    print(f"[sim] connecté à {args.host}:{args.port}, publication toutes les {args.interval}s/dispositif")

    try:
        while True:
            for device in devices:
                value = next_value(device)
                payload = {
                    "patientId": device["patientId"],
                    "deviceSerial": device["serial"],
                    "deviceType": device["deviceType"],
                    "loincCode": device["loincCode"],
                    "value": value,
                    "unit": device["unit"],
                    "ts": int(time.time() * 1000),
                }
                topic = f"caresync/devices/{device['serial']}/{TOPIC_BY_TYPE[device['deviceType']]}"
                client.publish(topic, json.dumps(payload), qos=0)
                print(f"[sim] -> {topic} : {value}{device['unit']} ({device['patientName']})")
            time.sleep(args.interval)
    except KeyboardInterrupt:
        pass
    finally:
        client.loop_stop()
        client.disconnect()

if __name__ == "__main__":
    main()
