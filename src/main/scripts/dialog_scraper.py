import requests
from bs4 import BeautifulSoup
import json
import time
import os
import re

# initial table method scraping
def scrape_npc_names():
    chisel_url = "https://chisel.weirdgloop.org/dialogue/"
    response = requests.get(chisel_url, verify=False)
    if response.status_code == 200:
        soup = BeautifulSoup(response.content, "html.parser")
        table = soup.find("table")
        if table:
            rows = table.find_all("tr")
            npc_names = []
            for row in rows[1:]:
                columns = row.find_all("td")
                npc_name = columns[0].text.strip()
                npc_names.append(npc_name)
            return npc_names
        else:
            print("NPC table not found!")
    else:
        print("HTTP request error:", response.status_code)
    return None

# Função para fazer o scraping dos diálogos de um NPC específico
def scrape_npc_dialogue(npc_name):
    url = f"https://chisel.weirdgloop.org/dialogue/npcs/{npc_name}"
    response = requests.get(url, verify=False)
    if response.status_code == 200:
        soup = BeautifulSoup(response.content, "html.parser")
        table = soup.find("table")
        if table:
            rows = table.find_all("tr")
            dialogue = {}
            total_chars = 0  # Total number of characters
            for row in rows[1:]:
                columns = row.find_all("td")
                message = columns[0].text.strip()
                count = columns[1].text.strip().replace(",", "")
                dialogue[message] = int(count)
                if isinstance(message, str):
                    total_chars += len(message)
            dialogue["total_chars"] = total_chars
            return dialogue
        else:
            print(f"Could not find dialog for '{npc_name}'!")
    else:
        print(f"HTTP request failed while scraping '{npc_name}'. Status code:", response.status_code)
    return None

if __name__ == "__main__":
    start_time = time.time()
    npc_names = scrape_npc_names()
    end_time = time.time()
    print(f"NPC scraping done. Time elapsed: {end_time - start_time:.2f} seconds.")

    transcripts = {}  # transcript dict that will be written to the json
    transcripts["transcript"] = {}

    scrape_count = 0
    start_time = time.time()
    total_chars = 0
    total_scrapes = len(npc_names)
    for npc_name in npc_names:
        scrape_count += 1
        dialogue = scrape_npc_dialogue(npc_name)
        transcripts["transcript"][npc_name] = {}

        if dialogue:
            chars = dialogue.pop("total_chars", 0)
            total_chars += chars
            print(f"Total characters in '{npc_name}': {chars} (total {total_chars})")

            for key, value in dialogue.items():
                if int(value) > 15:
                    text_key = re.sub(r"[\W\s]", "", key).lower()
                    transcripts["transcript"][npc_name][text_key] = key

    end_time = time.time()
    print(f"Done scraping. Time elapsed: {end_time - start_time:.2f} seconds.")

    # Save transcripts to a JSON file
    file_path = "transcripts.json"
    with open(file_path, "w") as file:
        json.dump(transcripts, file, indent=4)

    file_size = os.path.getsize(file_path)
    print(f"Done scraping. Transcripts saved!")
    print(f"File size: {file_size} bytes")
    print(f"Total scrapes done: {scrape_count}/{total_scrapes}")
