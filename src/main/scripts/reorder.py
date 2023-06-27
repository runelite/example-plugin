import json

# Abrir o arquivo transcripts.json
with open('transcripts.json', 'r') as file:
    data = json.load(file)

# Ordenar as chaves em ordem alfabética
sorted_keys = sorted(data['transcript'].keys())

# Criar um novo dicionário para armazenar os dados ordenados
sorted_data = {}

# Ordenar os valores dentro de cada chave em ordem alfabética
for key in sorted_keys:
    sorted_data[key] = sorted(data['transcript'][key])

# Salvar o JSON ordenado em um novo arquivo
with open('transcripts_sorted.json', 'w') as file:
    json.dump(sorted_data, file, indent=4)

print("JSON ordenado foi salvo no arquivo 'transcripts_sorted.json'.")
