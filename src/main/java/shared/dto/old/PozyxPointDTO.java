package shared.dto.old;

public class PozyxPointDTO {
    public int x = 0;
    public int y = 0;

    public PozyxPointDTO(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public PozyxPointDTO add(PozyxPointDTO p2) {
        return new PozyxPointDTO(x+p2.x, y+p2.y);
    }

    public PozyxPointDTO div(int num) {
        return new PozyxPointDTO(x/num, y/num);
    }

    public double dot(PozyxPointDTO p2) {
        return x*p2.x + y*p2.y;
    }

    public PozyxPointDTO sub(PozyxPointDTO p2) {
        return new PozyxPointDTO(x-p2.x, y-p2.y);
    }

    public double dist(PozyxPointDTO p2) {
        return Math.sqrt(Math.pow((x - p2.x), 2) + Math.pow((y - p2.y), 2));
    }

    public String toString() {
        return "{x: " + x + ", y:" + y + "}";
    }

    public static double getAngle(PozyxPointDTO a, PozyxPointDTO b, PozyxPointDTO c) {
        /*
        Returns the angle between the vectors ba and bc.
        Negative angle is anticlockwise
        Positive is clockwise
         */
        PozyxPointDTO ba = a.sub(b);
        PozyxPointDTO bc = b.sub(c);

        double thau1 = Math.atan2(ba.y, ba.x);
        double thau2 = Math.atan2(bc.y, bc.x);

//        return Math.toDegrees(thau1 - thau2) - 180;
        return Math.toDegrees(thau1 - thau2) + 180;

    }
}
