kill -9 $(lsof -i :8080 | cut -d ' ' -f 3 | tail -n 1)
