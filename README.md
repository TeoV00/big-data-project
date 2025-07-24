
Dataset Link --> [link](https://mcauleylab.ucsd.edu/public_datasets/gdrive/googlelocal/#sample-review)

Dataset sample is obtained using the following command:

```bash
pv -s 50G reviews.ndjson | awk 'BEGIN{srand(42)} rand()<=0.01 {print}' > sample.ndjson
```
